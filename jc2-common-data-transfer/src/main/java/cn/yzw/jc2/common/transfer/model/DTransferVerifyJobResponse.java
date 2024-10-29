package cn.yzw.jc2.common.transfer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @Description: 数据核对job
 * @Author: lbl 
 * @Date: 2024/10/24
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DTransferVerifyJobResponse {

    /**
     * 核对更新总数
     */
    AtomicLong verifyUpdateCount;
    /**
     * 核对删除总数
     */
    AtomicLong verifyDelCount;
    /**
     * 基于老表核对总数
     */
    Long       verifyOldTableCount;
    /**
     * 基于新表核对总数
     */
    Long       verifyNewTableCount;

}
