package com.easy.elastic.search.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ScrollResult<R> implements Serializable {

    private List<R> data;

    private String scrollId;

    private Long totalCount;
}
