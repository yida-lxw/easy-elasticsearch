package cn.yzw.jc2.common.transfer.service;

import java.util.List;
import java.util.Map;

import cn.yzw.jc2.common.transfer.model.ReadRequest;
import cn.yzw.jc2.common.transfer.model.WriteRequest;

/**
 * @description: 数据转换类
 * @author: yangle
 * @date: 2024/10/18
 **/
public interface DataTransferService {

    /**
     * 读取列
     * @param request
     * @return
     */
    List<Map<String, Object>> getDataList(ReadRequest request);

    /**
     * 获取最大id
     * @param table
     * @return
     */
    Long getMaxId(String table);

    /**
     * 批量执行
     * @param writeRequest
     */
    void doBatchInsert(WriteRequest writeRequest);

    /**
     * 单个执行
     * @param writeRequest
     */
    void doOneInsert(WriteRequest writeRequest);
}
