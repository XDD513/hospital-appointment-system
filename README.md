# 医院预约挂号系统 - 后端服务

## 📖 项目简介

基于 Spring Boot + MyBatis-Plus 的现代化医院预约挂号系统后端服务，提供完整的预约挂号、体质测试、健康档案、文章社区、消息通知、统计分析等业务能力，支持 JWT 安全认证与 WebSocket 实时通讯。

## ✨ 核心功能

### 1. 用户认证与授权
- 🔐 **JWT 认证** - 基于 Token 的无状态认证
- 🔑 **角色权限** - 患者、医生、管理员三级权限体系
- 🛡️ **Spring Security** - 安全框架集成
- 🔒 **密码加密** - BCrypt 密码加密存储

### 2. 预约挂号模块
- 📅 **排班管理** - 医生排班设置、号源管理
- 🏥 **预约管理** - 在线预约、预约审核、状态管理
- 📋 **号源查询** - 实时号源查询、余号统计
- ⏰ **预约提醒** - 预约成功、取消、提醒通知

### 3. 医患对话模块
- 💬 **实时对话** - WebSocket 实时消息推送
- 📎 **文件上传** - 图片、文档上传（阿里云 OSS）
- 📜 **消息历史** - 对话记录查询、消息管理
- 🔔 **消息通知** - 实时消息推送、未读消息统计

### 4. 健康管理模块
- 🧪 **体质测试** - 中医九种体质在线测试
- 📝 **健康档案** - 健康信息管理、历史记录
- ✅ **健康打卡** - 每日健康打卡、打卡记录
- 📊 **健康统计** - 健康数据统计分析

### 5. 内容管理模块
- 📚 **文章管理** - 养生文章发布、编辑、审核
- 🍲 **药膳推荐** - 药膳配方管理、个性化推荐
- 🎯 **穴位指导** - 穴位信息、按摩指导
- 🔍 **内容搜索** - 全文搜索、分类筛选

### 6. 评价反馈模块
- ⭐ **医生评价** - 患者对医生进行评价
- 💬 **评价回复** - 医生回复患者评价
- 📊 **评价统计** - 评价数据统计分析
- 🎯 **满意度调查** - 满意度评分、反馈收集

### 7. 系统管理模块
- 👥 **用户管理** - 用户信息管理、权限分配
- 🏥 **科室管理** - 科室信息维护、分类管理
- 👨‍⚕️ **医生管理** - 医生信息、资格审核
- ⚙️ **系统设置** - 系统配置、数据字典
- 📜 **操作日志** - 系统操作记录查询

### 8. 数据统计模块
- 📊 **预约统计** - 预约数据统计分析
- 📈 **用户统计** - 用户增长、活跃度统计
- 💰 **收入统计** - 预约收入、费用统计
- 📉 **趋势分析** - 数据趋势图表展示

## 🏗️ 技术架构

### 核心技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 2.7.18 | 应用框架 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| Spring Security | 2.7.18 | 安全框架 |
| Spring WebSocket | 2.7.18 | WebSocket 支持 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 6.0+ | 缓存数据库 |
| RabbitMQ | 3.8+ | 消息队列 |
| Nacos | 2.0+ | 配置中心/服务发现 |
| JWT | 0.11.5 | Token 认证 |
| Druid | 1.2.20 | 数据库连接池 |
| Hutool | 5.8.23 | Java 工具类库 |
| MapStruct | 1.5.5 | 对象映射 |
| EasyExcel | 3.3.2 | Excel 处理 |
| 阿里云 OSS | 3.17.4 | 对象存储 |
| OpenAI SDK | 0.18.2 | AI 对话（兼容 DeepSeek） |

### 主要依赖

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>3.5.5</version>
    </dependency>
    
    <!-- MySQL -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>
    
    <!-- Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- WebSocket -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    
    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    
    <!-- 更多依赖... -->
</dependencies>
```

## 📁 项目结构

```
hospital-appointment-system/
├── src/
│   ├── main/
│   │   ├── java/com/hospital/
│   │   │   ├── common/                # 公共模块
│   │   │   │   ├── config/            # 配置类
│   │   │   │   │   ├── CorsConfig.java
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   ├── RedisConfig.java
│   │   │   │   │   ├── RabbitMQConfig.java
│   │   │   │   │   └── WebSocketConfig.java
│   │   │   │   ├── constant/         # 常量定义
│   │   │   │   ├── exception/        # 异常处理
│   │   │   │   ├── result/           # 统一响应
│   │   │   │   └── util/             # 工具类
│   │   │   ├── controller/           # 控制器层
│   │   │   │   ├── UserController.java
│   │   │   │   ├── AppointmentController.java
│   │   │   │   ├── DoctorController.java
│   │   │   │   ├── ConversationController.java
│   │   │   │   └── ...
│   │   │   ├── service/              # 服务接口
│   │   │   │   ├── UserService.java
│   │   │   │   ├── AppointmentService.java
│   │   │   │   └── ...
│   │   │   ├── service/impl/         # 服务实现
│   │   │   │   ├── UserServiceImpl.java
│   │   │   │   ├── AppointmentServiceImpl.java
│   │   │   │   └── ...
│   │   │   ├── entity/              # 实体类
│   │   │   │   ├── User.java
│   │   │   │   ├── Appointment.java
│   │   │   │   ├── Doctor.java
│   │   │   │   └── ...
│   │   │   ├── mapper/              # Mapper 接口
│   │   │   │   ├── UserMapper.java
│   │   │   │   ├── AppointmentMapper.java
│   │   │   │   └── ...
│   │   │   └── HospitalApplication.java  # 启动类
│   │   └── resources/
│   │       ├── mapper/               # MyBatis XML 映射文件
│   │       │   ├── UserMapper.xml
│   │       │   ├── AppointmentMapper.xml
│   │       │   └── ...
│   │       ├── application.yml       # 全局配置
│   │       ├── application-dev.yml   # 开发环境配置
│   │       ├── application-prod.yml  # 生产环境配置
│   │       └── bootstrap.yml         # Nacos 配置
│   └── test/                         # 测试代码
│       └── java/com/hospital/
│           └── ...
├── sql/                              # SQL 脚本
│   ├── hospital_appointment_system.sql  # 数据库初始化
│   ├── tcm_health_system.sql        # 健康系统数据
│   ├── migrations/                  # 数据库迁移脚本
│   └── test-data/                   # 测试数据
├── docs/                            # 文档
│   └── 智能推荐模块.md
├── pom.xml                          # Maven 配置
└── README.md                        # 项目说明
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+（可选）
- Nacos 2.0+（可选，用于配置中心）

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE hospital_appointment_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入数据库脚本
mysql -u root -p hospital_appointment_system < sql/hospital_appointment_system.sql
mysql -u root -p hospital_appointment_system < sql/tcm_health_system.sql
```

### 2. 配置文件

编辑 `src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hospital_appointment_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
  
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### 3. 构建项目

```bash
cd hospital-appointment-system
mvn clean package -DskipTests
```

### 4. 运行项目

```bash
# 方式一：直接运行
java -jar target/hospital-appointment-system-1.0.0.jar

# 方式二：Maven 运行
mvn spring-boot:run
```

### 5. 验证运行

访问：http://localhost:8080

API 文档：http://localhost:8080/swagger-ui.html（如果配置了 Swagger）

## ⚙️ 配置说明

### 核心配置类

- `CorsConfig` - 跨域配置
- `SecurityConfig` - Spring Security 配置
- `RedisConfig` - Redis 配置
- `RabbitMQConfig` - RabbitMQ 配置
- `WebSocketConfig` - WebSocket 配置

### 配置文件

- `application.yml` - 全局配置
- `application-dev.yml` - 开发环境配置
- `application-prod.yml` - 生产环境配置
- `bootstrap.yml` - Nacos 配置（配置中心）

### JWT 配置

在 `application.yml` 中配置 JWT 密钥：

```yaml
jwt:
  secret: your-secret-key-here
  expiration: 86400000  # 24小时
```

### 阿里云 OSS 配置

```yaml
aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    bucket-name: your-bucket-name
```

## 🔌 API 接口

### 认证接口

- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/logout` - 用户登出
- `POST /api/auth/refresh` - 刷新 Token

### 预约接口

- `GET /api/appointment/list` - 获取预约列表
- `POST /api/appointment/create` - 创建预约
- `PUT /api/appointment/{id}` - 更新预约
- `DELETE /api/appointment/{id}` - 取消预约

### 医生接口

- `GET /api/doctor/list` - 获取医生列表
- `GET /api/doctor/{id}` - 获取医生详情
- `GET /api/doctor/schedule` - 获取排班信息

### 对话接口

- `GET /api/conversation/list` - 获取对话列表
- `POST /api/conversation/create` - 创建对话
- `GET /api/conversation/{id}/messages` - 获取消息列表
- `POST /api/conversation/{id}/message` - 发送消息

更多接口请参考代码中的 Controller 类。

## 🔌 WebSocket 实时通信

### 连接地址

```
ws://localhost:8080/ws
```

### 消息类型

- 会话消息推送
- 系统通知
- 预约提醒
- 评价提醒

### 订阅主题

```javascript
// 订阅个人消息
stompClient.subscribe('/user/queue/notification', (message) => {
  // 处理消息
});

// 订阅会话消息
stompClient.subscribe('/user/queue/conversation', (message) => {
  // 处理会话消息
});
```

## 📊 数据库设计

### 核心表结构

- `sys_user` - 用户表
- `tcm_doctor` - 医生表
- `tcm_appointment` - 预约表
- `tcm_schedule` - 排班表
- `tcm_conversation` - 对话表
- `tcm_message` - 消息表
- `tcm_constitution_test` - 体质测试表
- `tcm_health_profile` - 健康档案表
- `tcm_article` - 文章表
- `tcm_review` - 评价表

详细数据库设计请参考 `docs/数据库设计.md`

## 🧪 测试

### 运行单元测试

```bash
mvn test
```

### 运行集成测试

```bash
mvn verify
```

### 测试覆盖率

```bash
mvn test jacoco:report
```

## 📦 部署

### Docker 部署

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/hospital-appointment-system-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      - mysql
      - redis
  
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: hospital_appointment_system
  
  redis:
    image: redis:6-alpine
```

## 🐛 常见问题

### 1. 编译错误

确保使用 JDK 17，并在 `pom.xml` 中配置了正确的 Java 版本。

### 2. 数据库连接失败

检查数据库配置、用户名密码、数据库是否已创建。

### 3. Redis 连接失败

检查 Redis 服务是否启动，端口是否正确。

### 4. 跨域问题

检查 `CorsConfig` 配置，确保允许前端域名访问。

### 5. JWT 认证失败

检查 JWT 密钥配置，确保前后端使用相同的密钥。

## 📚 开发文档

- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [MyBatis-Plus 文档](https://baomidou.com/)
- [Spring Security 文档](https://spring.io/projects/spring-security)
- [Nacos 文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

MIT License

## 👥 作者

医院预约挂号系统后端开发团队

## 📧 联系方式

- 项目地址: [GitHub Repository](https://github.com/XDD513/hospital-appointment-system)
- 问题反馈: [GitHub Issues](https://github.com/XDD513/hospital-appointment-system/issues)
