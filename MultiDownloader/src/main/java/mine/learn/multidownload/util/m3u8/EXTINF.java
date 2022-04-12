package mine.learn.multidownload.util.m3u8;

import java.net.URI;
import java.util.Objects;

public class EXTINF {
    private float duration;
    private URI uri;

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EXTINF other = (EXTINF) obj;
        return Objects.equals(uri, other.uri);
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public EXTINF(float duration, URI uri) {
        this.duration = duration;
        this.uri = uri;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EXTINF [duration=").append(duration).append(", uri=").append(uri).append("]");
        return builder.toString();
    }

}