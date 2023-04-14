package cn.yzw.jc2.model;

import cn.yzw.jc2.enums.OperateTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 消息内容
 *
 * @author lbl
 * @date 2022/9/1 10:43
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message<E> implements Serializable {

    /** 1.0业务表主键 */
    private Long            sysNo;

    /** 操作类型 */
    private OperateTypeEnum operateType;

    /** 表名 */
    private String          table;

    /** 消息体 */
    private E               body;

}
