package cn.yzw.jc2.common.util.thread;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 命名的线程工厂，此线程工厂和由此创建出的线程的名称都具有实际可读意义
 *
 * @author liangbaole
 */
public class Jc2NamedThreadFactory implements ThreadFactory {

    /**
     * 线程池编号
     */
    private static final AtomicInteger threadPoolNumber = new AtomicInteger(1);

    /**
     * 线程组
     */
    private final ThreadGroup group;

    /**
     * 线程编号，在每个线程池里有序递增。
     */
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    /**
     * 线程名称前缀，模式为 线程池名称-pool-线程池编号-thread-
     */
    private final String namePrefix;

    /**
     * 创建的线程是否为守护线程
     */
    private boolean daemon;

    public Jc2NamedThreadFactory(String threadPoolName) {
        this(threadPoolName, false);
    }

    public Jc2NamedThreadFactory(String threadPoolName, boolean daemon) {
        SecurityManager s = System.getSecurityManager();
        group = Objects.nonNull(s) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        namePrefix = threadPoolName + "-pool-" + threadPoolNumber.getAndIncrement() + "-thread-";
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        thread.setDaemon(daemon);
        return thread;
    }
}
