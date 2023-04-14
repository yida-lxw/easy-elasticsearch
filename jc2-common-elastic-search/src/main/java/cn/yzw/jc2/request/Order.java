package cn.yzw.jc2.request;

import lombok.Data;

@Data
public class Order {

    private String orderByField;

    /**
     * ASC 升序，DESC降序
     */
    private String orderType = "DESC";
}
