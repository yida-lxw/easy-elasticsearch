package cn.yzw.jc2.common.search.request;

import java.io.Serializable;

import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * elasticsearch专用分页，request，支持深度下一页
 *
 * @Author lbl
 * @Date 2022/12/19 18:40
 * @Version 1.0
 */
@Data
@SuperBuilder
public class SearchPageRequest<E> extends SearchBaseRequest<E> implements Serializable {
    private int     pageNum  = 1;
    private int     pageSize = 10;

    /**
     * 是否睡眠
     */
    private Boolean sleep;

}
