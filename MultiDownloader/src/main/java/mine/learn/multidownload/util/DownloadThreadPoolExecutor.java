package mine.learn.multidownload.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mine.learn.multidownload.DownloadThread;
import mine.learn.multidownload.M3U8DownloadThread;

public class DownloadThreadPoolExecutor extends ThreadPoolExecutor {

    private final Set<DownloadThread> set;

    public Set<DownloadThread> getSet() {
        return set;
    }

    public DownloadThreadPoolExecutor(final int nThreads) {
        super(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        set = ConcurrentHashMap.newKeySet(nThreads);
    }

    @Override
    public void execute(final Runnable command) {
        if (command instanceof DownloadThread) {
            set.add((DownloadThread) command);
            super.execute(command);
        } else if (command instanceof M3U8DownloadThread) {
            super.execute(command);
        } else
            throw new UnsupportedOperationException("不能传入该类");
    }

}