package cn.yzw.jc2;


import cn.hutool.core.collection.ListUtil;
import cn.yzw.jc2.model.DMigrationJobQueryDO;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.List;

/**
 * @author liangbaole
 * @version 1.0
 * @desc
 * @ClassName DMigrationService
 * @date 2022/9/16 16:38
 */
public interface DMigrationService {

    /**
     * job全量数据迁移，分批查询id
     *
     * @param queryDO
     * @return
     */
    List<Long> queryList(DMigrationJobQueryDO queryDO);

    /**
     * 添加重试任务
     *
     * @param table
     * @param records
     * @return
     */
    default List<?> addRetryTaskIfNeed(String table, List<ConsumerRecord<String, String>> records) {
        return ListUtil.empty();
    }

    /**
     * @Description: 处理成功更新任务状态
     * @Author: lbl
     * @Date: 2023/4/10 15:27
     * @param:
     * @return:
     **/
    default void batchUpdateTask(List<?> taskModels) {

    }
}
