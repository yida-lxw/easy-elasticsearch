package com.easy.elastic.search.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {

    private String orderByField;

    /**
     * ASC 升序，DESC降序
     */
    private String orderType = "DESC";
}
