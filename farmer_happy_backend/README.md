# 农乐助农平台 - 后端服务

## 项目简介

农乐助农平台的后端服务，使用 Java 开发，基于原生 HTTP Server 实现 RESTful API。

## 技术栈

- **Java**: JDK 11+
- **构建工具**: Maven 3.x
- **数据库**: MySQL 8.x
- **HTTP 服务器**: Java 原生 HttpServer
- **依赖管理**: Maven

## 项目结构

```
farmer_happy_backend/
├── pom.xml                 # Maven 配置文件
├── src/
│   ├── main/
│   │   ├── java/          # Java 源代码
│   │   │   ├── application.java        # 应用程序入口
│   │   │   ├── config/                 # 配置类
│   │   │   ├── controller/             # 控制器层
│   │   │   ├── dto/                    # 数据传输对象
│   │   │   ├── entity/                 # 实体类
│   │   │   ├── repository/             # 数据访问层
│   │   │   └── service/                # 业务逻辑层
│   │   └── resources/     # 资源文件
│   └── test/
│       ├── java/          # 测试代码
│       └── resources/     # 测试资源
└── target/                # Maven 编译输出目录（自动生成）
```

## 环境要求

- JDK 11 或更高版本
- Maven 3.6 或更高版本
- MySQL 8.0 或更高版本

## 快速开始

### 1. 编译项目

```bash
mvn clean compile
```

### 2. 打包项目

```bash
mvn clean package
```

这将生成两个 JAR 文件：
- `target/farmer-happy-backend-1.0.0.jar` - 普通 JAR
- `target/farmer-happy-backend-1.0.0-standalone.jar` - 包含所有依赖的独立 JAR

### 3. 运行项目

#### 方式一：使用 Maven 插件运行（推荐用于开发）

```bash
mvn exec:java -Dexec.mainClass=application
```

#### 方式二：运行普通 JAR（需要依赖）

```bash
java -cp "target/farmer-happy-backend-1.0.0.jar;target/lib/*" application
```

#### 方式三：运行独立 JAR（推荐用于生产）

```bash
java -jar target/farmer-happy-backend-1.0.0-standalone.jar
```

### 4. 使用一键启动脚本

在项目根目录下运行：

```bash
start-all.bat
```

此脚本会自动编译并启动前后端服务。

## API 端点

服务默认运行在 `http://localhost:8080`

### 认证相关
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录

### 产品相关（农民）
- `GET /api/farmer/products` - 获取产品列表
- `POST /api/farmer/products` - 创建产品
- `GET /api/farmer/products/{id}` - 获取产品详情
- `PUT /api/farmer/products/{id}` - 更新产品
- `DELETE /api/farmer/products/{id}` - 删除产品
- `PATCH /api/farmer/products/{id}/status` - 更新产品状态

## Maven 常用命令

```bash
# 清理编译输出
mvn clean

# 编译项目
mvn compile

# 运行测试
mvn test

# 打包项目
mvn package

# 跳过测试打包
mvn package -DskipTests

# 安装到本地仓库
mvn install

# 查看依赖树
mvn dependency:tree

# 更新依赖
mvn versions:display-dependency-updates
```

## 开发说明

### 添加新依赖

在 `pom.xml` 的 `<dependencies>` 部分添加依赖：

```xml
<dependency>
    <groupId>groupId</groupId>
    <artifactId>artifactId</artifactId>
    <version>version</version>
</dependency>
```

### 数据库配置

数据库连接配置在 `repository/DatabaseManager.java` 中：

```java
private static final String URL = "jdbc:mysql://localhost:3306/farmer_happy";
private static final String USER = "root";
private static final String PASSWORD = "123456";
```

## 迁移说明

本项目已从手动编译方式迁移到 Maven 构建系统：

- ✅ 使用 Maven 管理依赖（MySQL Connector）
- ✅ 标准化项目结构（src/main/java）
- ✅ 自动化构建和打包
- ✅ 更新启动脚本使用 Maven 命令
- ✅ 支持多种运行方式

## 许可证

[项目许可证信息]

