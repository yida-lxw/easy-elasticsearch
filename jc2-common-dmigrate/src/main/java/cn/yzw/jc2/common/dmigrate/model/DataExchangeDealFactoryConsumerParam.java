package cn.yzw.jc2.common.dmigrate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataExchangeDealFactoryConsumerParam implements Serializable {
    private static final long         serialVersionUID = 3796458371711784852L;

    public String                     table;
    public List<StringConsumerRecord> records;
}
