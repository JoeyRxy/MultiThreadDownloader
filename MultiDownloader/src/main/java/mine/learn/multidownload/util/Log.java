package mine.learn.multidownload.util;

public class Log {
    private long id;
    private long alreadyDone;
    private final int activeThreadCount;
    private final int totalExecutedTreadCount;
    private double speed;

    /**
     * 
     * @param id                      秒数
     * @param alreadyDone             已经下载的大小
     * @param activeThreadCount       正在执行的线程数
     * @param totalExecutedTreadCount 已经执行过（包括正在执行）的线程数目
     * @param speed                   速度
     */
    public Log(final long id, final long alreadyDone, final int activeThreadCount, final int totalExecutedTreadCount,
            final double speed) {
        this.id = id;
        this.alreadyDone = alreadyDone;
        this.activeThreadCount = activeThreadCount;
        this.totalExecutedTreadCount = totalExecutedTreadCount;
        this.speed = speed;
    }

    /**
     * @param id the id to set
     */
    public void setId(final long id) {
        this.id = id;
    }

    /**
     * @param alreadyDone the alreadyDone to set
     */
    public void setAlreadyDone(final long alreadyDone) {
        this.alreadyDone = alreadyDone;
    }

    /**
     * @param speed the speed to set
     */
    public void setSpeed(final double speed) {
        this.speed = speed;
    }

    /**
     * 
     * @return id
     */
    public long getId() {
        return id;
    }

    /**
     * 
     * @return Byte
     */
    public long getAlreadyDone() {
        return alreadyDone;
    }

    /**
     * 
     * @return 活跃线程数
     */
    public int getActiveThreadCount() {
        return activeThreadCount;
    }

    /**
     * 
     * @return 已经执行过的线程数
     */
    public int getTotalExecutedTreadCount() {
        return totalExecutedTreadCount;
    }

    /**
     * 
     * @return Byte/s
     */
    public double getSpeed() {
        return speed;
    }

    @Override
    public String toString() {
        return String.format(
                "%d : Already Downloaded: %.2f KB, Speed: %.2f KB/s, Alive Thread Count: %d, Total Executed Thread: %d",
                id, alreadyDone / 1024., speed / 1024., activeThreadCount, totalExecutedTreadCount);
    }

}