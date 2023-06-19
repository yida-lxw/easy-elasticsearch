package cn.yzw.jc2.common.retry.config;

import cn.yzw.jc2.common.retry.job.RetryTaskJob;
import cn.yzw.jc2.common.retry.service.RetryTaskDomainService;
import cn.yzw.jc2.common.retry.service.impl.RetryTaskDomainImpl;
import cn.yzw.jc2.common.util.thread.Jc2NamedThreadFactory;
import cn.yzw.jc2.common.util.thread.ThreadPoolMdcWrapperExecutor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Data
@Configuration //表示这个类为配置类
public class RetryTaskConfig {
    @Value("${retry.task.timeout.seconds:1800}")
    private Long    retryTaskTimeoutSeconds;
    @Value("${retry.task.max.retry.times:60}")
    private Integer retryTaskMaxRetryTimes;
    @Value("${retry.task.page.size:10}")
    private Integer retryTaskPageSize;
    @Value("${spring.application.name}")
    private String  appName;

    /**
     * 重试任务表名
     */
    @Value("${retry.task.table.name}")
    private String  tableName;

    /**
     * 清理7天以前的数据
     */
    @Value("${retry.task.clean.day.rate:30}")
    private Long                     cleanBeforeDays;
    @Value("${retry.task.pool.core.size:10}")
    private Integer corePoolSize;
    @Value("${retry.task.pool.max.size:20}")
    private Integer maxPoolSize;
    @Value("${retry.task.pool.queue.size:500}")
    private Integer queuePoolSize;

    public Date getTimeOutStartTime() {
        return new Date(System.currentTimeMillis() - retryTaskTimeoutSeconds * 1000L);
    }

    @Bean("retryTaskThreadPoolTaskExecutor")
    public ThreadPoolExecutor retryTaskThreadPoolTaskExecutor() {
        //线程池满，抛出异常，等待下次调度
        ThreadPoolMdcWrapperExecutor taskExecutor = new ThreadPoolMdcWrapperExecutor(corePoolSize, maxPoolSize, 60,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(queuePoolSize), new Jc2NamedThreadFactory("retry-task"),
            new ThreadPoolExecutor.AbortPolicy());
        return taskExecutor;
    }

    @Bean("supRetryTaskDomainService")
    public RetryTaskDomainService SupRetryTaskDomainServiceImpl() {
        return new RetryTaskDomainImpl();
    }

    @Bean("retryTaskJob")
    public RetryTaskJob RetryTaskJob() {
        return new RetryTaskJob();
    }
}
