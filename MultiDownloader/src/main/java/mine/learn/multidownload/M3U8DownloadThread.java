package mine.learn.multidownload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import mine.learn.multidownload.util.PartInfo;
import mine.learn.multidownload.util.ThreadLog;

public class M3U8DownloadThread implements Runnable {
    private final PartInfo info;
    private long already = 0;
    private ThreadLog log;

    public M3U8DownloadThread(PartInfo info) {
        this.info = info;
    }

    @Override
    public void run() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultSocketConfig(Downloader.defaultSocketConfig)
                .setDefaultRequestConfig(Downloader.defaultRequestConfig).setDefaultHeaders(Downloader.headers);
        if (Downloader.proxy != null)
            builder.setProxy(Downloader.proxy);
        CloseableHttpClient client = builder.build();
        HttpGet get = new HttpGet(info.getUri());
        long contentLength = Long.MAX_VALUE;
        log = new ThreadLog(info.getId(), -1, 0, -1);
        File file = info.getFile();
        if (file.exists())
            file.delete();
        FileOutputStream out;
        try {
            out = new FileOutputStream(file, true);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return;
        }
        do {
            get.setHeader("Range", "bytes=" + already + "-");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpEntity entity = response.getEntity();
                if (log.getEnd() == -1) {
                    contentLength = entity.getContentLength();
                    log.setEnd(contentLength);
                }
                InputStream content = entity.getContent();
                byte[] b = new byte[65536];
                int len;
                while ((len = content.read(b)) != -1) {
                    out.write(b, 0, len);
                    already += len;
                    log.setIdx(already);
                    out.flush();
                    if (Downloader.PAUSED) {
                        synchronized (Downloader.lock) {
                            try {
                                Downloader.lock.wait();
                            } catch (final InterruptedException e) {
                                e.printStackTrace();
                                response.close();
                                out.close();
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (Downloader.threadListener != null)
                    Downloader.threadListener.log(log);
            }
        } while (contentLength < 0 || already < contentLength);
        try {
            out.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getAlready() {
        return already;
    }

    public long getContentLength() {
        return log.getEnd();
    }
}