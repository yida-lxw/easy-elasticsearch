package cn.yzw.jc2.common.search.result;

import lombok.Data;

import java.util.List;

@Data
public class SearchAfterResult<R> {

    private List<R>      data;

    private List<Object> searchAfterList;

    private Long         totalCount;
}
