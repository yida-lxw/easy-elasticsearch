package cn.yzw.jc2.common.transfer.model;

import lombok.Data;

@Data
public class DTransferDoubleWriteProperties {
    /**
     * 该表开启拦截，open开启，一下字段必填，没填报错
     */
    private Boolean open;
    /**
     * 老表名
     */
    private String  oldTableName;
    /**
     * 新表名
     */
    private String  newTableName;
    /**
     * 写类型，ONLY_WRITE_OLD_TABLE:写老表,ONLY_WRITE_NEW_TABLE：写新表,WRITE_ALL_TABLE：新老表都写
     */
    private String  writeType;

    /**
     * 读类型：READ_OLD_TABLE:读老表,READ_NEW_TABLE：读新表
     * 如果writeType=writeNewTable，强制读新表
     */
    private String  readType;
}
