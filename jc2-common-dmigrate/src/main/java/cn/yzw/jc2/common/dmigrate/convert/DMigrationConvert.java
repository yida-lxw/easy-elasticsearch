package cn.yzw.jc2.common.dmigrate.convert;

import cn.yzw.jc2.common.dmigrate.model.DMigrationJobRequest;
import cn.yzw.jc2.common.dmigrate.model.DMigrationJobQueryDO;
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
