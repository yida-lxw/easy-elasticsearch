package cn.yzw.jc2.common.util.log;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import cn.yzw.infra.component.base.constants.Constants;

/**
 * log 工具
 * @author yingxiang
 */
public class LogUtil {

    private final static String PREFIX_OF_REST_REQUEST   = "R";

    private final static String PREFIX_OF_RPC_REQUEST    = "D";

    private final static String PREFIX_OF_FEIGN_REQUEST  = "F";

    private final static String PREFIX_OF_JOB_REQUEST    = "J";
    private final static String PREFIX_OF_KAFKA_REQUEST  = "K";

    private final static String PREFIX_OF_MQ_REQUEST     = "Q";
    /** 回调 */
    private final static String PREFIX_OF_NOTIFY_REQUEST = "N";

    /**
     * rest访问放置上下文
     */
    public static void setNewTraceIdFromRest() {
        if (StringUtils.isEmpty(MDC.get(Constants.TRACE_MDC_KEY_LOG))) {
            MDC.put(Constants.TRACE_MDC_KEY_LOG, PREFIX_OF_REST_REQUEST + "-" + UUID.randomUUID().toString());
        }
    }

    /**
     * job上下文
     */
    public static void setNewTraceIdFromJob() {
        if (StringUtils.isEmpty(MDC.get(Constants.TRACE_MDC_KEY_LOG))) {
            MDC.put(Constants.TRACE_MDC_KEY_LOG, PREFIX_OF_JOB_REQUEST + "-" + UUID.randomUUID().toString());
        }
    }

    /**
     * kafka上下文
     */
    public static void setNewTraceIdFromKafka() {
        if (StringUtils.isEmpty(MDC.get(Constants.TRACE_MDC_KEY_LOG))) {
            MDC.put(Constants.TRACE_MDC_KEY_LOG, PREFIX_OF_KAFKA_REQUEST + "-" + UUID.randomUUID().toString());
        }
    }

    /**
     * kafka上下文
     */
    public static void setNewTraceIdFromMq() {
        if (StringUtils.isEmpty(MDC.get(Constants.TRACE_MDC_KEY_LOG))) {
            MDC.put(Constants.TRACE_MDC_KEY_LOG, PREFIX_OF_MQ_REQUEST + "-" + UUID.randomUUID().toString());
        }
    }

    /**
     * 回调上下文
     */
    public static void setNewTraceIdFromNotify() {
        if (StringUtils.isEmpty(MDC.get(Constants.TRACE_MDC_KEY_LOG))) {
            MDC.put(Constants.TRACE_MDC_KEY_LOG, PREFIX_OF_NOTIFY_REQUEST + "-" + UUID.randomUUID().toString());
        }
    }

    public static void removeTraceID() {
        MDC.remove(Constants.TRACE_MDC_KEY_LOG);
    }

    /**
     * setTraceId
     * @param traceId
     */
    public static void setTraceId(String traceId) {
        if (StringUtils.isNotBlank(traceId)) {
            MDC.put(Constants.TRACE_MDC_KEY_LOG, traceId);
        } else {
            MDC.remove(Constants.TRACE_MDC_KEY_LOG);
        }
    }

    /**
     * clear MDC
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * 获取当前线程的traceId
     * @return
     */
    public static String getTraceId() {
        return MDC.get(Constants.TRACE_MDC_KEY_LOG);
    }
}
