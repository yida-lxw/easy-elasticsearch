package cn.yzw.jc2.result;

import lombok.Data;

import java.util.List;

@Data
public class ScrollResult<R> {

    private List<R> data;

    private String scrollId;

    private Long totalCount;
}
