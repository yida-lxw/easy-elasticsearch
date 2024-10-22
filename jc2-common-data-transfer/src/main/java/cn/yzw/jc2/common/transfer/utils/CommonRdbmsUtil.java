package cn.yzw.jc2.common.transfer.utils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import cn.hutool.core.collection.CollectionUtil;
import cn.yzw.infra.component.base.exception.BusinessException;
import cn.yzw.infra.component.utils.AssertUtils;
import cn.yzw.jc2.common.transfer.model.DataBaseTypeEnum;

/**
 * @description: 读工具
 * @author: yangle
 * @date: 2024/10/21
 **/
public class CommonRdbmsUtil {

    public static String buildSqlLimit(DataBaseTypeEnum dataSourceType, String table, String sql, Long startId,
                                       Long endId, List<Long> idList, Integer limit) {
        switch (dataSourceType) {
            case MySql:
                if (StringUtils.isBlank(sql)) {
                    AssertUtils.notBlank(table, "sql语句为空时，必须传入表名");
                    sql = "select * from " + table;
                }
                StringBuilder sb = new StringBuilder(sql);
                sb.append(" id > ").append(Objects.nonNull(startId) ? startId : 0);
                if (Objects.nonNull(endId)) {
                    sb.append(" id <= ").append(endId);
                }
                if (CollectionUtil.isNotEmpty(idList)) {
                    sb.append("id in (").append(idList.stream().map(Object::toString).collect(Collectors.joining(",")))
                        .append(")");
                }
                sb.append(" limit ").append(Objects.nonNull(limit) ? limit : 20);
                return sb.toString();
            default:
                throw BusinessException.create("暂不支持该数据库类型" + dataSourceType);
        }
    }

//    public static Record buildRecord(ResultSet rs, ResultSetMetaData metaData, int columnNumber,
//                                     String mandatoryEncoding) {
//        try {
//            Record record = new DefaultRecord();
//            for (int i = 1; i < columnNumber; i++) {
//                switch (metaData.getColumnType(i)) {
//                    case Types.BIGINT:
//                        record.addColumn(new LongColumn(rs.getString(i)));
//                        break;
//                    case Types.VARCHAR:
//                        String rawData;
//                        if (StringUtils.isBlank(mandatoryEncoding)) {
//                            rawData = rs.getString(i);
//                        } else {
//                            rawData = new String((rs.getBytes(i) == null ? new byte[0] : rs.getBytes(i)),
//                                mandatoryEncoding);
//                        }
//                        record.addColumn(new StringColumn(rawData));
//                    case Types.DECIMAL:
//                        record.addColumn(new BigDecimalColumn(rs.getString(i)));
//                        break;
//                    case Types.DATE:
//                        record.addColumn(new DateColumn(rs.getDate(i)));
//                        break;
//                    case Types.TINYINT:
//                        record.addColumn(new BytesColumn(rs.getBytes(i)));
//                        break;
//                    case Types.BIT:
//                        record.addColumn(new BytesColumn(rs.getBytes(i)));
//                        break;
//                    case Types.TIMESTAMP:
//                        record.addColumn(new DateColumn(rs.getDate(i)));
//                        break;
//                    case Types.CHAR:
//                    case Types.NCHAR:
//                    case Types.LONGVARCHAR:
//                    case Types.NVARCHAR:
//                    case Types.LONGNVARCHAR:
//                    case Types.CLOB:
//                    case Types.NCLOB:
//                    case Types.SMALLINT:
//                    case Types.INTEGER:
//                    case Types.NUMERIC:
//                    case Types.FLOAT:
//                    case Types.REAL:
//                    case Types.DOUBLE:
//                    case Types.TIME:
//                    case Types.BINARY:
//                    case Types.VARBINARY:
//                    case Types.BLOB:
//                    case Types.LONGVARBINARY:
//                    case Types.BOOLEAN:
//                    case Types.NULL:
//                        String stringData = null;
//                        if (rs.getObject(i) != null) {
//                            stringData = rs.getObject(i).toString();
//                        }
//                        record.addColumn(new StringColumn(stringData));
//                        break;
//                    default:
//                        throw BusinessException.create(String.format(
//                            "不支持数据库读取这种字段类型. 字段名:[%s], 字段名称:[%s], 字段Java类型:[%s]. 请尝试使用数据库函数将其转换datax支持的类型 或者不同步该字段 .",
//                            metaData.getColumnName(i), metaData.getColumnType(i), metaData.getColumnClassName(i)));
//                }
//            }
//        } catch (Exception e) {
//            //TODO 这里识别为脏数据
//
//        }
//        return null;
//    }

//    public static Record buildOneRecord(Map<String, Object> data) {
//        try {
//            Record record = new DefaultRecord();
//            data.forEach((k, v) -> {
//                if (v instanceof String) {
//                    record.addColumn(new StringColumn(v.toString()));
//                }
//                if (v instanceof Boolean) {
//                    record.addColumn(new BoolColumn((Boolean) v));
//                }
//                if (v instanceof Timestamp) {
//                    record.addColumn(new DateColumn(new Date((((Timestamp) v).getTime()))));
//                }
//                if (v instanceof Long) {
//                    record.addColumn(new LongColumn((Long) v));
//                }
//                if (v instanceof BigDecimal) {
//                    record.addColumn(new BigDecimalColumn(v.toString()));
//                }
//            });
//            return record;
//        } catch (Exception e) {
//            //TODO 这里收集为异常数据
//
//        }
//        return null;
//    }

    public static String getWriteTemplate(List<String> columnHolders, List<String> valueHolders, String table) {

        return "INSERT INTO " + table + " (" + StringUtils.join(columnHolders, ",") + ") VALUES("
               + StringUtils.join(valueHolders, ",") + ")";
    }

    public static String onDuplicateKeyUpdateString(List<String> columnHolders) {
        if (columnHolders == null || columnHolders.size() < 1) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(" ON DUPLICATE KEY UPDATE ");
        boolean first = true;
        for (String column : columnHolders) {
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(column);
            sb.append("=VALUES(");
            sb.append(column);
            sb.append(")");
        }

        return sb.toString();
    }

}
