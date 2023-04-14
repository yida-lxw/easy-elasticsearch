package cn.yzw.jc2.common.dmigrate;

import cn.yzw.jc2.common.dmigrate.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * kafka数据消费转换策略
 *
 * @author lbl
 * @date 2022/9/1 14:37
 */
public interface ExchangeStrategyService<E, T> {
    Long   DEFAULT_USER_SYS_NO = -1L;
    String DEFAULT_USER_NAME   = "";

    /**
     * 获取泛化类型
     *
     * @return class
     */
    Class<E> getGenericType();

    /**
     * 写入2.0库
     *
     * @param list
     */
    void upsertBatch(List<T> list);

    /**
     * 物理删除操作
     *
     * @param list
     */
    default void batchDelete(List<Message<E>> list) {
    }

    /**
     *@desc 是否需要redis锁，默认需要，加锁逻辑为binlog主键id+表名
     * 如果不能满足自己业务场景，实现此方法，返回false,上游不再加redis分布式锁
     *@author liangbaole
     *@date 2022/9/24
     */
    default boolean isNeedRedisLock() {
        return true;
    }

    /**
     * @param >=minSysNo <maxSysNo limit每页条数
     * @param sysNoList
     * @desc 查询单表，不需要明细时，不需要实现
     * @author liangbaole
     * @date 2022/9/16
     */
    default List<E> queryListInterval(Long minSysNo, Long maxSysNo, Integer limit, List<Long> sysNoList) {
        throw new UnsupportedOperationException("请实现该方法queryListInterval");
    }

    /**
     * @desc 查询1.0库
     * @author liangbaole
     * @date 2022/9/14
     */
    List<T> queryDataList(List<Message<E>> messages);

    /**
     * 是否需要转换策略
     * @return topic list
     */
    default List<String> consumerTables() {
        return new ArrayList<>();
    }
}
