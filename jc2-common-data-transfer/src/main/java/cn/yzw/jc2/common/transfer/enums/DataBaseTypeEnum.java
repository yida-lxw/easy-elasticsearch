package cn.yzw.jc2.common.transfer.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * refer:http://blog.csdn.net/ring0hx/article/details/6152528
 * <p/>
 */
@Getter
@AllArgsConstructor
public enum DataBaseTypeEnum {
    MySql("mysql", "com.mysql.jdbc.Driver"),
    Tddl("mysql", "com.mysql.jdbc.Driver"),
    DRDS("drds", "com.mysql.jdbc.Driver"),
    Oracle("oracle", "oracle.jdbc.OracleDriver"),
    SQLServer("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    PostgreSQL("postgresql", "org.postgresql.Driver"),
    RDBMS("rdbms", "com.alibaba.datax.plugin.rdbms.util.DataBaseType"),
    DB2("db2", "com.ibm.db2.jcc.DB2Driver"),
    ADB("adb", "com.mysql.jdbc.Driver"),
    ADS("ads", "com.mysql.jdbc.Driver"),
    ClickHouse("clickhouse", "ru.yandex.clickhouse.ClickHouseDriver"),
    KingbaseES("kingbasees", "com.kingbase8.Driver"),
    Oscar("oscar", "com.oscar.Driver"),
    OceanBase("oceanbase", "com.alipay.oceanbase.jdbc.Driver"),
    StarRocks("starrocks", "com.mysql.jdbc.Driver"),
    Sybase("sybase", "com.sybase.jdbc4.jdbc.SybDriver"),
    GaussDB("gaussdb", "org.opengauss.Driver"),
    Databend("databend", "com.databend.jdbc.DatabendDriver"),
    Doris("doris", "com.mysql.jdbc.Driver");

    private String typeName;
    private String driverClassName;
}
