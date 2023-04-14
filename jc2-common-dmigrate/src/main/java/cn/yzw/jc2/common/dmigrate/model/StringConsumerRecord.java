package cn.yzw.jc2.common.dmigrate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.record.TimestampType;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StringConsumerRecord implements Serializable {
    private static final long serialVersionUID = 3518262339639432025L;

    public String             topic;
    public int                partition;
    public long               offset;
    public long               timestamp;
    public TimestampType      timestampType;
    public int                serializedKeySize;
    public int                serializedValueSize;
    public String             key;
    public String             value;
    public Long               checksum;

}
