package mine.learn.multidownload;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import mine.learn.multidownload.util.ThreadLog;

public class DownloadThread implements Runnable {

    private final CloseableHttpClient client;
    private final int id;
    private final long start;
    private long end;
    /** 下一个将要下载的字节 */
    private long idx;

    private boolean pause;

    private final ThreadLog log;

    private static RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(2000)
            .setConnectTimeout(2000).setConnectionRequestTimeout(2000).build();
    private static SocketConfig defaultSocketConfig = SocketConfig.custom().setSoKeepAlive(false).setSoLinger(1)
            .setSoReuseAddress(true).setSoTimeout(2000).setTcpNoDelay(true).build();

    private static HttpRequestRetryHandler retryHandler = (exception, executionCount, context) -> {
        return false;
    };

    /**
     * 下载[{@code start}, {@code end})部分的内容
     *
     * @param id    下载线程的id，用以区分不同的线程，按时间的先后顺序升序
     * @param start 线程下载开始的位置(included)
     * @param end   线程下载的结束位置(excluded)
     * @throws IOException
     */
    public DownloadThread(final int id, final long start, final long end) throws IOException {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultHeaders(Downloader.headers).setDefaultRequestConfig(defaultRequestConfig)
                .setRetryHandler(retryHandler).setDefaultSocketConfig(defaultSocketConfig);
        if (Downloader.proxy != null) {
            builder.setProxy(Downloader.proxy);
        }
        client = builder.build();
        this.id = id;
        this.start = start;
        this.end = end;
        this.idx = start;
        log = new ThreadLog(id, start, idx, end);
    }

    @Override
    public void run() {
        final HttpGet get = new HttpGet(Downloader.uri);
        try {
            RandomAccessFile rf = new RandomAccessFile(Downloader.file, "rw");
            rf.seek(start);
            long remain;
            while (true) {
                get.setHeader("Range", "bytes=" + idx + "-" + (end - 1));
                try (CloseableHttpResponse response = client.execute(get)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode < 200 || statusCode >= 300)
                        throw new IOException();
                    // out = new FileOutputStream(rf, true);
                    final InputStream in = response.getEntity().getContent();
                    int len;
                    final byte[] b = new byte[(int) Math.min(end - idx, 65536)];
                    while (true) {
                        len = in.read(b);
                        if (len == -1)
                            break;
                        remain = end - idx;
                        if (remain <= len) {
                            rf.write(b, 0, (int) remain);
                            idx += remain;
                            log.setIdx(idx);
                            break;
                        }
                        rf.write(b, 0, len);
                        idx += len;
                        log.setIdx(idx);
                        if (Downloader.PAUSED) {
                            synchronized (Downloader.lock) {
                                try {
                                    Downloader.lock.wait();
                                } catch (final InterruptedException e) {
                                    e.printStackTrace();
                                    response.close();
                                    rf.close();
                                    return;
                                }
                            }
                        }
                        if (pause) {
                            if (remain() <= 1048576 || Downloader.executor.isShutdown())
                                continue;
                            long mid = (end + idx) >> 1;
                            long _pEnd = end;
                            end = mid;
                            try {
                                Downloader.executor.execute(
                                        new DownloadThread(Downloader.totalThread.getAndIncrement(), mid, _pEnd));
                            } catch (RejectedExecutionException e) {
                                System.err.println("该异常只应出现在调用Downloader.killAll()方法之后，否则，本代码出现了问题");
                                e.printStackTrace();
                            }
                            pause = false;
                        }
                    }
                    // in.close();//IMPORTANT The difference between closing the content stream and
                    // closing
                    // the response is that the former will attempt to keep the underlying
                    // connection alive by consuming the entity content while the latter immediately
                    // shuts down and discards the connection.
                    break;
                } catch (final IOException e) {
                    if (Downloader.threadListener != null)
                        Downloader.threadListener.log(log);
                }
            }
            if (idx < end)
                System.err.println("fucked");
            rf.close();
            client.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the end
     */
    public long getEnd() {
        return end;
    }

    /**
     * @return the start
     */
    public long getStart() {
        return start;
    }

    public void pauseAndSplit() {
        this.pause = true;
    }

    /**
     * @return 当前线程已经下载的大小(Byte)
     */
    public long alreadyDone() {
        return idx - start;
    }

    /**
     * @return 当前线程未下载的部分的大小(Byte)
     */
    public long remain() {
        return end - idx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DownloadThread other = (DownloadThread) obj;
        return id == other.id;
    }

}