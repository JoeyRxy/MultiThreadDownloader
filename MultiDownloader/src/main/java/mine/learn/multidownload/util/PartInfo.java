package mine.learn.multidownload.util;

import java.io.File;
import java.net.URI;
import java.util.Objects;

public class PartInfo {
    private int id;
    private float seconds;
    private URI uri;
    private File file;

    public int getId() {
        return id;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getSeconds() {
        return seconds;
    }

    public void setSeconds(float seconds) {
        this.seconds = seconds;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

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
        PartInfo other = (PartInfo) obj;
        return Objects.equals(uri, other.uri);
    }

    public PartInfo() {
    }

    public PartInfo(int id, float seconds, URI uri, File file) {
        this.id = id;
        this.seconds = seconds;
        this.uri = uri;
        this.file = file;
    }

}