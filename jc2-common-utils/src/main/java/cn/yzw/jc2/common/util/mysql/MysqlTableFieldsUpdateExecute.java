package cn.yzw.jc2.common.util.mysql;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: mysql修改更新字段
 * @Author: lbl 
 * @Date: 2024/5/9
 **/
@Slf4j
public class MysqlTableFieldsUpdateExecute {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 睡眠间隙，单位：ms，默认10ms
     */
    @Value("${mysql.table.field.sleep.gap:10}")
    private Long         sleepGap;
    /**
     * // 每页查询返回的记录数
     */
    @Value("${mysql.table.field.page.size:100}")
    private Integer      pageSize;

    /**
     * @Description: 按表的维度，遍历查询，批量更新数据库
     * @Author: lbl
     * @Date: 2024/5/8 15:26
     * @param:
     * @return:
     **/
    public void execute(UpdateTableParams updateTableParams) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("每页大小不能小于等于0");
        }
        Long maxId = selectMaxId(updateTableParams.getTableName());
        if (maxId == null) {
            return;
        }

        // 上一次查询结果中最后一个ID
        long lastId = updateTableParams.getStartId() == null ? 0 : updateTableParams.getStartId();

        while (lastId <= maxId) {
            List<Long> ids = select(updateTableParams.getTableName(), lastId, pageSize);
            if (CollectionUtils.isEmpty(ids)) {
                log.info("MysqlTableFieldsUpdateExecute.execute最后执行id:{}", lastId);
                break;
            }
            // 处理查询结果
            update(ids, updateTableParams);

            // 更新lastId
            lastId = ids.get(ids.size() - 1);

            try {
                if (sleepGap > 0) {
                    Thread.sleep(sleepGap);
                }
            } catch (InterruptedException e) {
                log.warn("MysqlTableFieldsUpdateExecute.execute线程异常", e);
            }
        }
    }

    /**
     * @Description: 更新
     * @Author: lbl
     * @Date: 2024/5/8 15:26
     * @param:
     * @return:
     **/
    public int update(List<Long> ids, UpdateTableParams updateTableParams) {
        String set = " ";
        for (int i = 0; i < updateTableParams.getFieldList().size(); i++) {
            UpdateTableParams.UpdateField updateField = updateTableParams.getFieldList().get(i);
            if (UpdateFieldTypeEnum.FIELD.name().equals(updateField.getUpdateFieldType())) {
                set = set + updateField.getTargetFieldName() + " = " + updateField.getOriginFieldName();
            } else if (UpdateFieldTypeEnum.VALUE.name().equals(updateField.getUpdateFieldType())) {
                if (updateField.getTargetValue() instanceof String) {
                    set = set + updateField.getTargetFieldName() + " = " + "\'" + updateField.getTargetValue() + "\'";
                } else {
                    set = set + updateField.getTargetFieldName() + " = " + updateField.getTargetValue();
                }
            }
            if (i < ids.size() - 1) {
                set += ", ";
            }
        }
        // 构建 SQL 语句
        String sql = "UPDATE " + updateTableParams.getTableName() + " SET " + set + " WHERE id IN (";

        // 拼接参数
        for (int i = 0; i < ids.size(); i++) {
            sql += "?";
            if (i < ids.size() - 1) {
                sql += ", ";
            }
        }
        sql += ")";
        // 执行更新操作
        return jdbcTemplate.update(sql, ids.toArray());
    }

    /**
     * @Description: 批次查询
     * @Author: lbl 
     * @Date:  2024/5/9 11:43
     * @param:
     * @return:
     **/
    public List<Long> select(String table, Long lastId, Integer pageSize) {
        return jdbcTemplate.query("SELECT id FROM " + table + " WHERE id > ? ORDER BY id ASC LIMIT ?",
            new Object[] { lastId, pageSize }, (rs, rowNum) -> rs.getLong("id"));
    }

    /**
     * @Description: 查询最大值
     * @Author: lbl 
     * @Date:  2024/5/9 11:44
     * @param:
     * @return:
     **/
    public Long selectMaxId(String table) {
        return jdbcTemplate.queryForObject("SELECT max(id) FROM " + table, Long.class);
    }
}
