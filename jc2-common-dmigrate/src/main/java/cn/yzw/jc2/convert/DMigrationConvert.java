package cn.yzw.jc2.convert;

import cn.yzw.jc2.model.DMigrationJobQueryDO;
import cn.yzw.jc2.model.DMigrationJobRequest;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;


/**
 * @author liangbaole
 * @version 1.0
 * @desc
 * @ClassName DMigrationConvert
 * @date 2022/9/16 14:57
 */
@Mapper
public interface DMigrationConvert {
    DMigrationConvert INSTANCE = Mappers.getMapper(DMigrationConvert.class);

    DMigrationJobQueryDO convert(DMigrationJobRequest request);
}
