package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话Mapper
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}


