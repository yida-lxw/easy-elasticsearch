package cn.yzw.jc2.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.skywalking.apm.toolkit.trace.CallableWrapper;
import org.apache.skywalking.apm.toolkit.trace.RunnableWrapper;
import org.slf4j.MDC;

import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;

/**
 * @Description: 多线程执行，子线程拿不到traceId，
 * 此线程池重写ThreadPoolTaskExecutor，将traceId传递下去
 * @Author: lbl
 * @Date: 2023/1/17
 **/
public class ThreadPoolMdcWrapperExecutor extends ThreadPoolExecutor {

    public void execute(Runnable task) {
        // 使用阿里巴巴ttl修饰，以传递上下文
        Runnable r = TtlRunnable.get(task);
        super.execute(RunnableWrapper.of(ThreadMdcUtil.wrap(r, MDC.getCopyOfContextMap())));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        // 使用阿里巴巴ttl修饰，以传递上下文
        Callable<T> c = TtlCallable.get(task);
        return super.submit(CallableWrapper.of(ThreadMdcUtil.wrap(c, MDC.getCopyOfContextMap())));
    }

    /**
     * @param task the {@code Runnable} to execute (never {@code null})
     * @return
     */
    @Override
    public Future<?> submit(Runnable task) {
        // 使用阿里巴巴ttl修饰，以传递上下文
        Runnable r = TtlRunnable.get(task);
        return super.submit(RunnableWrapper.of(ThreadMdcUtil.wrap(r, MDC.getCopyOfContextMap())));
    }

    public ThreadPoolMdcWrapperExecutor() {
        super(5, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
    }

    public ThreadPoolMdcWrapperExecutor(ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(5, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), threadFactory, handler);
    }

    public ThreadPoolMdcWrapperExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public ThreadPoolMdcWrapperExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public ThreadPoolMdcWrapperExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public ThreadPoolMdcWrapperExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                                        RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

}
