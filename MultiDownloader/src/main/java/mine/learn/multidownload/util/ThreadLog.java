package mine.learn.multidownload.util;

/**
 * 单个线程的日志
 */
public class ThreadLog {
    private int id;
    private long start;
    private long idx;
    private long end;

    /**
     * 默认日志输出
     */
    @Override
    public String toString() {
        return String.format("%s : Connection Reset. Retrying... Already Done Of this thread : %.2f KB / %.2f KB",
                "Thread " + id, (idx - start) / 1024., (end - start) / 1024.);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getIdx() {
        return idx;
    }

    public void setIdx(long idx) {
        this.idx = idx;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    /**
     * 单线程日志
     * 
     * @param id    时间（秒）
     * @param start 开始下载位置
     * @param idx   当前下载位置
     * @param end   结束下载位置
     */
    public ThreadLog(int id, long start, long idx, long end) {
        this.id = id;
        this.start = start;
        this.idx = idx;
        this.end = end;
    }

}