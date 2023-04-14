package cn.yzw.jc2.thread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinTask;

import cn.yzw.infra.component.base.constants.Constants;
import org.slf4j.MDC;

/**
 * @Description: 线程traceId上下文设置
 * @Author: lbl 
 * @Date: 2023/1/17
 **/
public class ThreadMdcUtil {
    public static void setTraceIdIfAbsent() {
        if (MDC.get(Constants.TRACE_MDC_KEY_LOG) == null) {
            MDC.put(Constants.TRACE_MDC_KEY_LOG, MDC.get(Constants.TRACE_MDC_KEY_LOG));
        }
    }

    public static <T> Callable<T> wrap(final Callable<T> callable, final Map<String, String> context) {
        return () -> {
            if (context == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(context);
            }
            setTraceIdIfAbsent();
            try {
                return callable.call();
            } finally {
                MDC.clear();
            }
        };
    }

    public static <T> List<Callable<T>> wrapList(Collection<? extends Callable<T>> tasks,
                                                 final Map<String, String> context) {
        List<Callable<T>> list = new ArrayList<>();
        tasks.forEach(task -> {
            Callable<T> t = () -> {
                if (context == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(context);
                }
                setTraceIdIfAbsent();
                try {
                    return task.call();
                } finally {
                    MDC.clear();
                }
            };
            list.add(t);
        });
        return list;
    }

    public static Runnable wrap(final Runnable runnable, final Map<String, String> context) {
        return () -> {
            if (context == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(context);
            }
            setTraceIdIfAbsent();
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }

    public static <T> ForkJoinTask<T> wrap(final ForkJoinTask<T> joinTask, final Map<String, String> context) {
        ForkJoinTask<T> task = new ForkJoinTask<T>() {
            @Override
            public T getRawResult() {
                return null;
            }

            @Override
            protected void setRawResult(T value) {

            }

            @Override
            protected boolean exec() {
                return false;
            }
        };
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
        setTraceIdIfAbsent();
        try {
            task.invoke();
        } finally {
            MDC.clear();
        }
        return task;
    }

}
