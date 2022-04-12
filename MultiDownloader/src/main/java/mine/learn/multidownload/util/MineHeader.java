package mine.learn.multidownload.util;

import org.apache.http.message.BasicHeader;

public class MineHeader extends BasicHeader {

    /**
     *
     */
    private static final long serialVersionUID = -4919486474991017428L;

    public MineHeader(String name, String value) {
        super(name, value);
    }

    @Override
    public int hashCode() {
        return super.getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return getName().equals(((MineHeader) obj).getName());
    }

}
