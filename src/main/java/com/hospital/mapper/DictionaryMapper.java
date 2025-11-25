package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.Dictionary;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据字典Mapper接口
 */
@Mapper
public interface DictionaryMapper extends BaseMapper<Dictionary> {
}
