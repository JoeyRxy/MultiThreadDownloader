package mine.learn.multidownload;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;

import org.apache.http.Header;
import org.apache.http.HttpHost;

import mine.learn.multidownload.util.MineHeader;

/**
 * DownloaderBuilder
 */
public class DownloaderBuilder {

    private static final MineHeader DEFAULT_USER_AGENT = new MineHeader("User-Agnet",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.122 Safari/537.36");
    private File file;
    private File tmpFile;
    private URI uri;
    private int threadNum;
    private HashSet<Header> headers;
    private HttpHost proxy;
    private File m3u8File;
    private URI m3u8BaseUri;

    public static DownloaderBuilder create() {
        return new DownloaderBuilder();
    }

    private DownloaderBuilder() {
        threadNum = 8;
        headers = new HashSet<>();
        headers.add(DEFAULT_USER_AGENT);
    }

    public final DownloaderBuilder setFile(File file) {
        this.file = file;
        return this;
    }

    public final DownloaderBuilder setFile(String file) {
        this.file = new File(file);
        return this;
    }

    public final DownloaderBuilder setTempFileRoot(File tmpRoot) {
        tmpFile = tmpRoot;
        return this;
    }

    public final DownloaderBuilder setTempFileRoot(String tmpRoot) {
        tmpFile = new File(tmpRoot);
        return this;
    }

    public final DownloaderBuilder setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public final DownloaderBuilder setUri(String uri) {
        this.uri = URI.create(uri);
        return this;
    }

    public final DownloaderBuilder setThreadNum(int threadNum) {
        this.threadNum = threadNum;
        return this;
    }

    public final DownloaderBuilder setHeaders(Collection<Header> headers) {
        this.headers.clear();
        this.headers.addAll(headers);
        return this;
    }

    public final DownloaderBuilder addHeader(Header header) {
        headers.remove(header);
        headers.add(header);
        return this;
    }

    public final DownloaderBuilder addHeaders(Collection<Header> addiHeaders) {
        headers.removeAll(addiHeaders);
        headers.addAll(addiHeaders);
        return this;
    }

    public final DownloaderBuilder setM3U8File(File m3u8File) {
        this.m3u8File = m3u8File;
        return this;
    }

    public final DownloaderBuilder setM3U8BaseUri(URI uri) {
        this.m3u8BaseUri = uri;
        return this;
    }

    public final DownloaderBuilder setM3U8BaseUriStr(String uri) {
        this.m3u8BaseUri = URI.create(uri);
        return this;
    }

    public final DownloaderBuilder setProxy(HttpHost proxy) {
        this.proxy = proxy;
        return this;
    }

    public final DownloaderBuilder setReferer(String refererStr) {
        MineHeader header = new MineHeader("referer", refererStr);
        headers.remove(header);
        headers.add(header);
        return this;
    }

    public final DownloaderBuilder setUserAgent(String userAgent) {
        MineHeader header = new MineHeader("User-Agent", userAgent);
        headers.remove(header);
        headers.add(header);
        return this;
    }

    public final DownloaderBuilder setReferer(URI refererUri) {
        MineHeader header = new MineHeader("referer", refererUri.toString());
        headers.remove(header);
        headers.add(header);
        return this;
    }

    public final DownloaderBuilder setCookie(String cookie) {
        MineHeader header = new MineHeader("Cookie", cookie);
        headers.remove(header);
        headers.add(header);
        return this;
    }

    public Downloader build() throws IOException {
        if (file == null)
            throw new IllegalArgumentException("<file> for downloading must be set.");
        if (tmpFile == null)
            tmpFile = new File(file.getParent(), "tmp");
        Downloader.file = file;
        Downloader.threadNum = threadNum;
        Downloader.proxy = proxy;
        Downloader.headers = headers;
        if (uri != null) {
            Downloader.uri = uri;
            return new Downloader(tmpFile);
        } else if (m3u8File != null) {
            return new Downloader(tmpFile, m3u8File, m3u8BaseUri);
        } else
            throw new IllegalArgumentException("<uri> must be set, or <m3u8File> at least.");
    }

}