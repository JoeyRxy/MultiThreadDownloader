package mine.learn.multidownload.util.m3u8;

import java.net.URI;

public class EXTKEY {
    private String method;
    private URI uri;
    private String iv;

    public EXTKEY() {
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public EXTKEY(String method, URI uri, String iv) {
        this.method = method;
        this.uri = uri;
        this.iv = iv;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EXTKEY [iv=").append(iv).append(", method=").append(method).append(", uri=").append(uri)
                .append("]");
        return builder.toString();
    }

}