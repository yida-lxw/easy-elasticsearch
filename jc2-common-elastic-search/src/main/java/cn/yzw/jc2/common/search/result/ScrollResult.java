package cn.yzw.jc2.common.search.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ScrollResult<R> implements Serializable {

    private List<R> data;

    private String scrollId;

    private Long totalCount;
}
