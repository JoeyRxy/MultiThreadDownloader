package mine.learn.multidownload.util.m3u8;

import java.util.LinkedList;
import java.util.List;

public class M3U8Info {
    private List<EXTINF> extInfos = new LinkedList<>();
    private EXTKEY key;

    public M3U8Info() {

    }

    public List<EXTINF> getExtInfos() {
        return extInfos;
    }

    public void add(EXTINF info) {
        extInfos.add(info);
    }

    public EXTKEY getKey() {
        return key;
    }

    public void setKey(EXTKEY key) {
        this.key = key;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        // builder.append("M3U8Info [extInfos=").append(extInfos).append(",
        // key=").append(key).append("]");
        builder.append("key = ").append(key).append("\nInfos:\n");
        for (EXTINF extinf : extInfos) {
            builder.append(extinf).append('\n');
        }
        return builder.toString();
    }

}