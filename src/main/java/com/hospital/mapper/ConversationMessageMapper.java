package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.ConversationMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话消息Mapper
 */
@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {
}


