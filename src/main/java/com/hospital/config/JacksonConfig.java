package com.hospital.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;

import java.math.BigInteger;

/**
 * 全局Jackson配置：将Long/long/BigInteger序列化为字符串，避免前端JS精度丢失
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            SimpleModule longToStringModule = new SimpleModule();
            longToStringModule.addSerializer(Long.class, ToStringSerializer.instance);
            longToStringModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
            longToStringModule.addSerializer(BigInteger.class, ToStringSerializer.instance);
            // 安装自定义Long/BigInteger序列化模块与JavaTimeModule，避免覆盖Spring Boot默认模块
            builder.modulesToInstall(longToStringModule, new JavaTimeModule());
            // 关闭将日期写成时间戳，统一使用字符串格式
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}