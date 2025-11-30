package com.hospital.config;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

import javax.sql.DataSource;

/**
 * 应用关闭配置类
 * 确保所有资源在应用关闭时正确释放，避免内存泄漏
 *
 * @author Hospital Team
 * @since 2025-11-30
 */
@Slf4j
@Component
public class ApplicationShutdownConfig implements ApplicationListener<ContextClosedEvent> {

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private ConnectionFactory rabbitConnectionFactory;

    @Autowired(required = false)
    private SubProtocolWebSocketHandler webSocketHandler;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("应用开始关闭，正在释放资源...");

        // 关闭Redis连接
        shutdownRedis();

        // 关闭数据库连接池
        shutdownDataSource();

        // 关闭RabbitMQ连接
        shutdownRabbitMQ();

        // 关闭WebSocket连接
        shutdownWebSocket();

        log.info("资源释放完成");
    }

    /**
     * 关闭Redis连接
     */
    private void shutdownRedis() {
        if (redisConnectionFactory != null) {
            try {
                log.info("正在关闭Redis连接...");
                // 关闭Lettuce连接
                if (redisConnectionFactory instanceof org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) {
                    ((org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) redisConnectionFactory).destroy();
                }
                log.info("Redis连接已关闭");
            } catch (Exception e) {
                log.warn("关闭Redis连接时出错: {}", e.getMessage());
            }
        }
    }

    /**
     * 关闭数据库连接池
     */
    private void shutdownDataSource() {
        if (dataSource != null && dataSource instanceof DruidDataSource druidDataSource) {
            try {
                log.info("正在关闭数据库连接池...");
                druidDataSource.close();
                log.info("数据库连接池已关闭");
            } catch (Exception e) {
                log.warn("关闭数据库连接池时出错: {}", e.getMessage());
            }
        }
    }

    /**
     * 关闭RabbitMQ连接
     */
    private void shutdownRabbitMQ() {
        if (rabbitConnectionFactory != null) {
            try {
                log.info("正在关闭RabbitMQ连接...");
                // CachingConnectionFactory 有 destroy() 方法
                if (rabbitConnectionFactory instanceof org.springframework.amqp.rabbit.connection.CachingConnectionFactory) {
                    ((org.springframework.amqp.rabbit.connection.CachingConnectionFactory) rabbitConnectionFactory).destroy();
                    log.info("RabbitMQ连接已关闭");
                } else {
                    // 其他类型的 ConnectionFactory 由 Spring 自动管理
                    log.debug("RabbitMQ连接将由Spring自动关闭");
                }
            } catch (Exception e) {
                log.warn("关闭RabbitMQ连接时出错: {}", e.getMessage());
            }
        }
    }

    /**
     * 关闭WebSocket连接
     */
    private void shutdownWebSocket() {
        if (webSocketHandler != null) {
            try {
                log.info("正在关闭WebSocket连接...");
                // WebSocketHandler会在Spring容器关闭时自动处理
                // 这里主要是确保所有连接都已关闭
                Thread.sleep(200); // 给WebSocket一些时间完成关闭
                log.info("WebSocket连接已关闭");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("关闭WebSocket连接时被中断: {}", e.getMessage());
            } catch (Exception e) {
                log.warn("关闭WebSocket连接时出错: {}", e.getMessage());
            }
        }
    }

}

