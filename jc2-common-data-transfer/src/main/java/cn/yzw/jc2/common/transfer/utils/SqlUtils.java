package cn.yzw.jc2.common.transfer.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.values.ValuesStatement;

public class SqlUtils {
    // 使用正则表达式替换 SQL 中的表名
    public static String replaceTableName2(String sql, String oldTableName, String newTableName) {
        String regex = "\\b" + oldTableName + "\\b"; // 匹配整个表名，避免部分匹配
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        return matcher.replaceAll(newTableName);
    }
    public static String replaceTableName(String sql, String oldTableName, String newTableName) throws Exception {
        // 解析 SQL 语句
        Statement statement = CCJSqlParserUtil.parse(sql);

        // 替换表名的处理
        if (statement instanceof Select) {
            Select select = (Select) statement;
            select.getSelectBody().accept(new SelectVisitor() {
                @Override
                public void visit(PlainSelect plainSelect) {
                    replaceTableNameInFromItem(plainSelect.getFromItem(), oldTableName, newTableName);
                    if (plainSelect.getJoins() != null) {
                        for (Join join : plainSelect.getJoins()) {
                            replaceTableNameInFromItem(join.getRightItem(), oldTableName, newTableName);
                        }
                    }
                }

                @Override
                public void visit(SetOperationList setOpList) {

                }

                @Override
                public void visit(WithItem withItem) {

                }

                @Override
                public void visit(ValuesStatement aThis) {

                }
            });
        } else if (statement instanceof Update) {
            Update update = (Update) statement;
            replaceTableNameInFromItem(update.getTable(), oldTableName, newTableName);
        } else if (statement instanceof Insert) {
            Insert insert = (Insert) statement;
            replaceTableNameInFromItem(insert.getTable(), oldTableName, newTableName);
        } else if (statement instanceof Delete) {
            Delete delete = (Delete) statement;
            replaceTableNameInFromItem(delete.getTable(), oldTableName, newTableName);
        }

        // 返回修改后的 SQL 语句
        return statement.toString();
    }

    private static void replaceTableNameInFromItem(FromItem fromItem, String oldTableName, String newTableName) {
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            if (table.getName().equalsIgnoreCase(oldTableName)) {
                table.setName(newTableName);
            }
        }
    }
}
