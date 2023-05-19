package cn.yzw.jc2.common.search.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private String orderByField;

    /**
     * ASC 升序，DESC降序
     */
    private String orderType = "DESC";
}
