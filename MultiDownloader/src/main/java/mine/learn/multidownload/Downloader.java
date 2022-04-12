package mine.learn.multidownload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import mine.learn.multidownload.util.DownloadThreadPoolExecutor;
import mine.learn.multidownload.util.Log;
import mine.learn.multidownload.util.LogListener;
import mine.learn.multidownload.util.MineRetryHandler;
import mine.learn.multidownload.util.PartInfo;
import mine.learn.multidownload.util.ThreadLogListener;
import mine.learn.multidownload.util.m3u8.EXTINF;
import mine.learn.multidownload.util.m3u8.EXTKEY;
import mine.learn.multidownload.util.m3u8.M3U8FileParser;
import mine.learn.multidownload.util.m3u8.M3U8Info;

/**
 * 支持动态线程开启，即每个爬取线程时可动态再次开启多个线程。后期速度不至于下降过快
 * <p>
 * 支持两种多线程下载方式：
 * <ol>
 * <li>普通文件
 * <li>HLS文件（不完全支持，只支持m3u8文件里列出了所有子文件的形式）；且支持加密的HLS（AES-128）
 * </ol>
 * <p>
 * changelog 5.0:
 * <p>
 * 1.
 * 将文件下载（相对于M3U8下载而言）的方式变为直接写入最终文件，节省了merge()每个线程的文件的时间（在机械硬盘电脑上merge大量文件还是很费时的）。但仍然保留了形式参数{@code tmpRoot}，在M3U8文件的下载时，仍然需要将分文件下载完成后再合并。
 * <p>
 * 2. 修改了计算"alreadyDone"(已下载)的方法，其返回准确的大小（但下载时仍有不超过64KB/线程 的冗余下载）
 * <p>
 * 
 * @author Rxy
 * @version 5.0
 */
public class Downloader {

    private final static String SECURITY_CAUSE = "you should use open-jdk to run the app, otherwise, like oracle's java enviroment, will leads to this error. the oracle can't use some security packages for some security reasons.";
    private static final String INTERRUPT_CAUSE = "Some event interrupts the downloading thread, downloading failed.";
    private static final String FILE_CAUSE = "Maybe the file path or temp-file path went wrong.";
    private static final String INTERNET_CAUSE = "Internet had some problem";

    protected static final Object lock = new Object();

    protected static boolean PAUSED = false;
    protected static File file;
    private CloseableHttpClient client;
    protected static URI uri;
    private long contentLength = -1;
    protected static int threadNum;
    private boolean done = false;

    protected static HttpHost proxy;
    protected static HashSet<Header> headers;
    protected static DownloadThreadPoolExecutor executor;
    protected static AtomicInteger totalThread = new AtomicInteger();
    private LogListener logListener;

    private double speed;
    protected static ThreadLogListener threadListener;

    protected static RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(20000)
            .setConnectTimeout(20000).setConnectionRequestTimeout(20000).build();
    protected static SocketConfig defaultSocketConfig = SocketConfig.custom().setSoKeepAlive(false).setSoLinger(1)
            .setSoReuseAddress(true).setSoTimeout(20000).setTcpNoDelay(true).build();
    private int statusCode;

    private boolean isM3U8;
    private List<PartInfo> infos;
    private List<M3U8DownloadThread> lists;

    private boolean valid;

    private Cipher cipher = null;

    /**
     * 本方法中不进行下载。在运行之后，能够得到下载长度{@code contentLength}，该结果可以为负，表示文件大小未知。
     * <p>
     * 当{@code file}中已经有内容时，自动退出，并且后续的下载不进行。
     * <p>
     * 当{@code file}所指定的目录不存在时，创建该路径。
     * 
     * @param tmpRoot M3U8文件的各个部分文件的存储目录
     * @throws IOException IO错误
     */
    protected Downloader(File tmpRoot) throws IOException {
        if (!tmpRoot.exists())
            tmpRoot.mkdirs();
        if (file.exists() && file.length() > 0) {
            System.err.println("Already Exist!");
            return;
        }
        final File parentFile = file.getAbsoluteFile().getParentFile();
        if (!parentFile.exists())
            parentFile.mkdirs();
        if (headers != null && headers.size() > 0) {
            Downloader.headers.addAll(headers);
        }
        final HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultHeaders(Downloader.headers).setDefaultRequestConfig(Downloader.defaultRequestConfig)
                .setRetryHandler(new MineRetryHandler()).setDefaultSocketConfig(Downloader.defaultSocketConfig);
        if (proxy != null) {
            builder.setProxy(proxy);
        }
        client = builder.build();
        final HttpGet resume = new HttpGet(uri);
        try {
            if (uri.toString().contains(".m3u8")) {
                isM3U8 = true;
                CloseableHttpResponse response = client.execute(resume);
                String m3u8String = EntityUtils.toString(response.getEntity());
                response.close();
                String url = uri.toString();
                url = url.substring(0, url.lastIndexOf("/") + 1);
                infos = new LinkedList<>();
                M3U8Info m3u8Info = M3U8FileParser.parse(m3u8String, url);
                if (m3u8Info == null) {
                    System.err.println("M3U8 File Parser Error!");
                    return;
                }
                EXTKEY key = m3u8Info.getKey();
                if (key != null) {
                    String pwd = EntityUtils.toString(client.execute(new HttpGet(key.getUri())).getEntity());
                    byte[] ivBytes = null;
                    if (key.getIv() != null)
                        if (key.getIv().startsWith("0x"))
                            ivBytes = hexStringToByteArray(key.getIv().substring(2));
                        else
                            ivBytes = key.getIv().getBytes();
                    Security.addProvider(new BouncyCastleProvider());
                    cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
                    SecretKeySpec secretKeySpec = new SecretKeySpec(pwd.getBytes(), "AES");
                    if (ivBytes != null) {
                        if (ivBytes.length != 16)
                            ivBytes = new byte[16];
                        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
                        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
                    } else {
                        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
                    }
                }
                int id = 0;
                for (EXTINF extInfo : m3u8Info.getExtInfos()) {
                    infos.add(new PartInfo(id++, extInfo.getDuration(), extInfo.getUri(),
                            new File(tmpRoot, file.getName() + "part" + id)));
                }
                valid = true;
                client.close();
                return;
            }
            resume.setHeader("Range", "bytes=0-");

            final CloseableHttpResponse response = client.execute(resume);
            statusCode = response.getStatusLine().getStatusCode();
            contentLength = response.getEntity().getContentLength();
            response.close();
            valid = true;
        } catch (IOException e) {
            throw new IOException(INTERNET_CAUSE);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException
                | NoSuchProviderException | NoSuchPaddingException e) {
            throw new SecurityException(SECURITY_CAUSE);
        }
    }

    /**
     * 通过输入m3u8文件的方式直接下载，不同于另一种构造方法（直接导入链接，并通过链接下载m3u8文件之后再下载）
     * 
     * @param tmpRoot  M3U8文件的各个部分文件的存储目录
     * @param m3u8File m3u8文件
     * @param baseURI  下载的基本url，是m3u8文件的列表中的各个小文件的下载链接的前缀
     */
    protected Downloader(File tmpRoot, File m3u8File, URI baseURI) {
        // Downloader.threadNum = threadNum;
        // Downloader.file = file;
        // Downloader.proxy = proxy;
        // Downloader.headers = headers;
        try (BufferedReader reader = new BufferedReader(new FileReader(m3u8File))) {
            isM3U8 = true;
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            M3U8Info m3u8Info = M3U8FileParser.parse(builder.toString(), baseURI.toString());
            if (m3u8Info == null) {
                System.err.println("M3U8 File Parser Error!");
                return;
            }
            builder = null;
            EXTKEY key = m3u8Info.getKey();
            if (key != null) {
                String pwd = EntityUtils.toString(client.execute(new HttpGet(key.getUri())).getEntity());
                byte[] ivBytes;
                if (key.getIv().startsWith("0x"))
                    ivBytes = hexStringToByteArray(key.getIv().substring(2));
                else
                    ivBytes = key.getIv().getBytes();
                Security.addProvider(new BouncyCastleProvider());
                cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
                SecretKeySpec secretKeySpec = new SecretKeySpec(pwd.getBytes(), "AES");
                IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            }
            infos = new LinkedList<>();
            int id = 0;
            for (EXTINF extInfo : m3u8Info.getExtInfos()) {
                infos.add(new PartInfo(id++, extInfo.getDuration(), extInfo.getUri(),
                        new File(tmpRoot, file.getName() + "part" + id)));
            }
            valid = true;
            client.close();
        } catch (Exception e) {
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if ((len & 1) == 1)
            throw new IllegalArgumentException("length should be even number.");
        byte[] data = new byte[len >> 1];
        for (int i = 0; i < len; i += 2) {
            data[i >> 1] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * 主下载进程
     * <p>
     * <ol>
     * <li>当文件已存在或者构造方法中出现了错误时，不进行下载</li>
     * <li>进行206测试（部分下载测试），当不支持部分下载时，使用单线程进行尝试</li>
     * <li>如果上述情况没有出现，可进行多线程下载</li>
     * </ol>
     * 
     * @throws IOException               合并文件失败
     * @throws InterruptedException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public void run() throws IOException, InterruptedException, IllegalBlockSizeException, BadPaddingException {
        if (!valid)
            return;
        if (isM3U8) {
            totalThread.set(Math.min(threadNum, infos.size()));
            executor = new DownloadThreadPoolExecutor(totalThread.get());
            lists = new ArrayList<>(infos.size() + 10);
            for (PartInfo partInfo : infos) {
                M3U8DownloadThread command = new M3U8DownloadThread(partInfo);
                lists.add(command);
                executor.execute(command);
            }
            executor.shutdown();
            long prev = 0, already, i = 0;
            try {
                while (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    already = alreadyDone();
                    speed = (already - prev) / 2.;
                    if (logListener != null) {
                        logListener.log(new Log(i, already, executor.getActiveCount(), totalThread.get(), speed));
                        i += 2;
                    }
                    prev = already;
                }
            } catch (InterruptedException e) {
                throw new InterruptedException(INTERRUPT_CAUSE);
            }
            System.out.println("===== Merging M3U8 Files =====");
            try {
                FileOutputStream out = new FileOutputStream(file, true);
                for (PartInfo info : infos) {
                    byte[] b = new byte[(int) info.getFile().length()];
                    FileInputStream in = new FileInputStream(info.getFile());
                    in.read(b);
                    if (cipher != null)
                        out.write(cipher.doFinal(b));
                    else
                        out.write(b);
                    in.close();
                }
                out.close();
            } catch (IOException e) {
                throw new IOException(FILE_CAUSE);
            } catch (IllegalBlockSizeException e) {
                throw new IllegalBlockSizeException(SECURITY_CAUSE);
            } catch (BadPaddingException e) {
                throw new BadPaddingException(SECURITY_CAUSE);
            }
            done = true;
            for (PartInfo info : infos) {
                info.getFile().delete();
            }
            return;
        }
        int support = supportResume();
        if (support == 2)
            return;
        if (support == 0)
            threadNum = 1;
        try {
            client.close();
        } catch (final IOException e) {
            System.err.println("这个错误有点不可能出现，再试一次！");
            e.printStackTrace();
        }
        threadNum = Math.min((int) Math.ceil(contentLength / 262144.), threadNum);
        totalThread.set(threadNum);
        final long size = contentLength / threadNum;
        long start = 0, end = size;
        executor = new DownloadThreadPoolExecutor(threadNum);
        for (int i = 0; i < threadNum - 1; i++) {
            executor.execute(new DownloadThread(i, start, end));
            start = end;
            end += size;
        }
        executor.execute(new DownloadThread(threadNum - 1, start, contentLength));
        boolean isMore = true, over;
        int activeCount;
        long prev = 0, downloaded;
        long i = 0;
        while (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
            downloaded = alreadyDone();
            speed = (downloaded - prev) / 2.;
            if (logListener != null) {
                logListener.log(new Log(i, downloaded, executor.getActiveCount(), totalThread.get(), speed));
                i += 2;
            }
            activeCount = executor.getActiveCount();
            if (isMore && activeCount < threadNum) {
                over = true;
                for (final DownloadThread downloadThread : executor.getSet())
                    if (activeCount < threadNum) {
                        if (downloadThread.remain() > 1048576) {
                            over = false;
                            ++activeCount;
                            downloadThread.pauseAndSplit();
                        }
                    } else
                        break;
                if (over) {
                    isMore = false;
                    executor.shutdown();
                    continue;
                }
            }
            prev = downloaded;
        }
        done = true;
    }

    /*
     * private void merge() throws IOException { if (file.exists()) file.delete();
     * final FileOutputStream out = new FileOutputStream(file, true); final
     * TreeSet<DownloadThread> tmpFileSet = new TreeSet<>(executor.getSet()); for
     * (final DownloadThread thread : tmpFileSet) { final File tmp =
     * thread.getTmpFile(); final FileInputStream in = new FileInputStream(tmp); int
     * len; byte[] b = new byte[65536]; while ((len = in.read(b)) != -1)
     * out.write(b, 0, len); out.flush(); in.close(); try { tmp.delete(); } catch
     * (final SecurityException e) { System.err.println("无法删除 " + tmp.getName() +
     * "，请手动删除"); e.printStackTrace(); } } out.close(); }
     */

    /**
     * 
     * @return 整个已经下载的部分的大小(Byte)
     */
    public long alreadyDone() {
        if (isM3U8) {
            long res = 0;
            for (M3U8DownloadThread list : lists) {
                res += list.getAlready();
            }
            return res;
        }
        long alreadySize = 0;
        for (final DownloadThread downloadThread : executor.getSet()) {
            alreadySize += downloadThread.alreadyDone();
        }
        return alreadySize;
    }

    private int supportResume() throws IOException, InterruptedException {
        if (contentLength <= 0) { // 不知道长度
            System.out.println("暂不支持多点下载，可能是网络问题，尝试单线程下载中...");
            final ExecutorService singleExecutor = Executors.newSingleThreadExecutor();
            final Log log = new Log(0, 0, 1, 1, 0);
            singleExecutor.execute(() -> {
                final HttpGet singleGet = new HttpGet(uri);
                int tryingTimes = 3;
                long idx = 0;
                boolean isOver = false;
                while (tryingTimes-- > 0 && !isOver) {
                    try (CloseableHttpResponse download = client.execute(singleGet)) {
                        final InputStream content = download.getEntity().getContent();
                        final FileOutputStream out = new FileOutputStream(file);
                        final byte[] b = new byte[8192];
                        int len;
                        final long start = System.currentTimeMillis();
                        while ((len = content.read(b)) != -1) {
                            out.write(b, 0, len);
                            out.flush();
                            idx += len;
                            log.setAlreadyDone(idx);
                            if (PAUSED) {
                                synchronized (lock) {
                                    lock.wait();
                                }
                            }
                        }
                        out.close();
                        isOver = true;
                        System.out.println("单线程下载成功，耗时：" + (System.currentTimeMillis() - start) / 1000 + " s");
                    } catch (final IOException e) {
                        System.err.println("Retrying..." + (3 - tryingTimes));
                        idx = 0;
                        log.setAlreadyDone(0);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            singleExecutor.shutdown();
            long prev = 0;
            long i = 0;
            try {
                while (!singleExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.setId(i);
                    i += 2;
                    final long alreadyDone = log.getAlreadyDone();
                    if (alreadyDone > prev)
                        log.setSpeed(alreadyDone - prev);
                    else
                        log.setSpeed(0);
                    prev = alreadyDone;
                    if (logListener != null)
                        logListener.log(log);
                }
            } catch (InterruptedException e) {
                throw new InterruptedException(INTERRUPT_CAUSE);
            }
            return 2;
        }
        if (statusCode == 206)
            return 1;
        else if (statusCode >= 200 && statusCode < 300)
            return 0;
        else
            throw new IOException(INTERNET_CAUSE + " status code: " + statusCode);

    }

    public LogListener getLogListener() {
        return logListener;
    }

    /**
     * @return the headers
     */
    public Collection<Header> getHeaders() {
        return headers;
    }

    /**
     * 
     * @param logListener 整个下载过程的日志记录
     */
    public void registerLogListener(final LogListener logListener) {
        this.logListener = logListener;
    }

    /**
     * @return the proxy
     */
    public HttpHost getProxy() {
        return proxy;
    }

    /**
     * @return the file
     */
    public File getFile() {
        return file;
    }

    /**
     * @return whether done
     */
    public boolean isDone() {
        return done;
    }

    /**
     * @return the speed
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * @return the thread number
     */
    public int getThreadNumber() {
        return threadNum;
    }

    /**
     * @return the executor
     */
    public DownloadThreadPoolExecutor getExecutor() {
        return executor;
    }

    /**
     * @return the contentLength
     */
    public long getContentLength() {
        if (contentLength == -1)
            return -1;
        if (isM3U8) {
            long len = 0;
            for (M3U8DownloadThread thread : lists) {
                len += thread.getContentLength();
            }
            return len;
        }
        return contentLength;
    }

    /**
     * 暂停下载
     */
    public void pauseAll() {
        PAUSED = true;
    }

    /**
     * 重新开始
     */
    public void resumeAll() {
        if (PAUSED) {
            PAUSED = false;
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    /**
     * 停止下载
     * <p>
     * 使用interrupt方法，未必能够终止
     */
    public void killAll() {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
    }

    public void registerThreadRetryLog(final ThreadLogListener threadListener) {
        Downloader.threadListener = threadListener;
    }

}
