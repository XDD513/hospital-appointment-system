package com.hospital;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 医院预约系统启动类
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@SpringBootApplication
@EnableScheduling
@EnableRabbit
@MapperScan("com.hospital.mapper")
public class HospitalApplication {

    public static void main(String[] args) {
        SpringApplication.run(HospitalApplication.class, args);
    }
}

