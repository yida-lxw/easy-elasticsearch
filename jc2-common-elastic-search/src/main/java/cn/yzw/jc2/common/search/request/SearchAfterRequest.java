package cn.yzw.jc2.common.search.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * elasticsearch专用分页，request，支持深度下一页
 *
 * @Author lbl
 * @Date 2022/12/19 18:40
 * @Version 1.0
 */
@Data
public class SearchAfterRequest<E> extends SearchBaseRequest<E> implements Serializable {
    /**
     * 每页大小
     */
    private int          pageSize = 100;
    /**
     * "search_after的字段值的列表，最后一个元素必须是“唯一有序不为空”字段，推荐主键id"
     */
    private List<Object> searchAfterList;

}
