# 🧪 FarmerHappy 后端测试指南

本文档说明如何使用 JUnit 5 进行单元测试。

## 📋 目录

- [测试框架配置](#测试框架配置)
- [测试目录结构](#测试目录结构)
- [运行测试](#运行测试)
- [编写测试](#编写测试)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)

---

## 🔧 测试框架配置

项目已配置以下测试框架和工具：

### 核心依赖

- **JUnit 5** (5.10.1) - 测试框架
- **Mockito** (5.7.0) - 模拟对象框架
- **AssertJ** (3.24.2) - 流式断言库
- **H2 Database** (2.2.224) - 测试用内存数据库

---

## 📁 测试目录结构

```
farmer_happy_backend/
├── src/
│   ├── main/java/           # 主代码
│   └── test/
│       ├── java/            # 测试代码
│       │   ├── controller/
│       │   ├── service/
│       │   ├── entity/
│       │   └── dto/
│       └── resources/       # 测试资源
│           ├── junit-platform.properties
│           └── mockito-extensions/
├── pom.xml
└── README-TESTING.md (本文档)
```

---

## 🚀 运行测试

### 方法一：使用 Maven 命令

```bash
# 运行所有测试
cd farmer_happy_backend
mvn test

# 运行特定测试类
mvn test -Dtest=UserTest

# 运行特定包下的所有测试
mvn test -Dtest=entity.*

# 运行特定测试方法
mvn test -Dtest=UserTest#testNicknameGetterSetter

# 跳过测试
mvn package -DskipTests
```

### 方法二：使用 VSCode

1. **安装扩展**
   - 安装 "Extension Pack for Java"（包含测试支持）
   - 或单独安装 "Test Runner for Java"

2. **运行测试**
   - 打开测试文件，点击方法上方的 `▶ Run Test` 按钮
   - 或在测试资源管理器（Testing）面板中运行
   - 快捷键：`Ctrl + ; A` 运行所有测试

3. **调试测试**
   - 点击 `🐛 Debug Test` 按钮
   - 或在测试方法上设置断点后运行

### 方法三：使用测试脚本

**Windows:**
```bash
run-tests.bat
```

**Linux/Mac:**
```bash
chmod +x run-tests.sh
./run-tests.sh
```

---

## ✍️ 编写测试

### 测试类命名规范

- 测试类名 = 被测试类名 + `Test`
- 例如: `UserService` → `UserServiceTest`
- 测试类放在与被测试类相同的包结构中

### 基本测试模板

```java
package entity;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("用户实体测试")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    @DisplayName("测试昵称设置")
    void testNickname() {
        // Given (准备)
        String nickname = "张三";
        
        // When (执行)
        user.setNickname(nickname);
        
        // Then (验证)
        assertThat(user.getNickname()).isEqualTo(nickname);
    }
}
```

### 使用 Mockito 模拟依赖

```java
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

class ServiceTest {

    @Mock
    private DatabaseManager databaseManager;
    
    private AuthService authService;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        authService = new AuthServiceImpl(databaseManager);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testFindUser() {
        // 模拟数据库返回
        User mockUser = new User();
        when(databaseManager.findUser("test")).thenReturn(mockUser);

        // 测试
        User result = authService.findUser("test");

        // 验证
        assertThat(result).isNotNull();
        verify(databaseManager, times(1)).findUser("test");
    }
}
```

### 参数化测试

```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

@ParameterizedTest
@ValueSource(strings = {"13800138000", "13912345678", "15800000000"})
void testValidPhones(String phone) {
    assertThat(isValidPhone(phone)).isTrue();
}

@ParameterizedTest
@CsvSource({
    "张三, 13800138000, true",
    "李四, 13912345678, true",
    "王五, 12345, false"
})
void testUserValidation(String name, String phone, boolean expected) {
    boolean result = validateUser(name, phone);
    assertThat(result).isEqualTo(expected);
}
```

### 异常测试

```java
import static org.junit.jupiter.api.Assertions.*;

@Test
void testExceptionThrown() {
    assertThrows(IllegalArgumentException.class, () -> {
        validatePhone(null);
    });
}

@Test
void testExceptionMessage() {
    Exception exception = assertThrows(
        IllegalArgumentException.class,
        () -> validatePhone(null)
    );
    
    assertThat(exception.getMessage()).contains("电话号码不能为空");
}
```

---

## 🎯 最佳实践

### 1. 遵循 AAA 模式

```java
@Test
void testUserCreation() {
    // Arrange (准备)
    String nickname = "测试用户";
    
    // Act (执行)
    User user = new User();
    user.setNickname(nickname);
    
    // Assert (断言)
    assertThat(user.getNickname()).isEqualTo(nickname);
}
```

### 2. 使用描述性名称

```java
@Test
@DisplayName("当电话号码为null时应该抛出异常")
void shouldThrowExceptionWhenPhoneIsNull() { }
```

### 3. 测试边界条件

```java
@Test
void testEdgeCases() {
    assertThat(validate(null)).isFalse();           // null
    assertThat(validate("")).isFalse();             // 空字符串
    assertThat(validate("  ")).isFalse();           // 空白
    assertThat(validate("a")).isFalse();            // 最小值-1
    assertThat(validate("abc")).isTrue();           // 有效值
}
```

### 4. 保持测试独立

```java
// ✅ 好的做法
@BeforeEach
void setUp() {
    user = new User(); // 每个测试都有新实例
}

// ❌ 不好的做法
private static User user = new User(); // 共享状态
```

### 5. 使用 AssertJ 提高可读性

```java
// ✅ 使用 AssertJ - 更易读
assertThat(user.getNickname())
    .as("昵称应该匹配")
    .isNotNull()
    .isEqualTo("张三");

// 普通 JUnit 断言
assertTrue(user.getNickname() != null);
assertEquals("张三", user.getNickname());
```

### 6. 推荐的测试覆盖率

- **Service 层**: 80-90%
- **Controller 层**: 70-80%
- **Entity/DTO**: 60-70%

---

## 📊 查看测试报告

### Maven Surefire 报告

运行测试后，报告位于：
```
farmer_happy_backend/target/surefire-reports/
```

### 生成 HTML 报告

```bash
mvn surefire-report:report
```
报告位置：`target/site/surefire-report.html`

### 测试覆盖率报告（可选）

如需覆盖率报告，可在 `pom.xml` 中添加 JaCoCo 插件：

```bash
mvn clean test jacoco:report
```
报告位置：`target/site/jacoco/index.html`

---

## ❓ 常见问题

### 1. 测试编译失败

```bash
# 清理并重新下载依赖
mvn clean install
```

### 2. Mock 不生效

确保正确初始化 Mockito：
```java
@BeforeEach
void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
}

@AfterEach
void tearDown() throws Exception {
    closeable.close();
}
```

### 3. 参数化测试不运行

确保已添加依赖：
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-params</artifactId>
    <version>5.10.1</version>
    <scope>test</scope>
</dependency>
```

### 4. 测试运行缓慢

```bash
# 并行运行测试
mvn test -DforkCount=4
```

---

## 🎉 快速开始

1. **查看示例测试**
   - `src/test/java/entity/UserTest.java`
   - `src/test/java/dto/auth/LoginRequestDTOTest.java`
   - `src/test/java/service/auth/AuthServiceImplTest.java`

2. **运行测试**
   ```bash
   cd farmer_happy_backend
   mvn test
   ```

3. **编写自己的测试**
   - 在 `src/test/java` 下创建测试类
   - 参考示例代码
   - 运行并验证

---

## 💡 核心要点

- ✅ 测试应该快速、独立、可重复
- ✅ 使用有意义的测试名称和 @DisplayName
- ✅ 遵循 AAA (Arrange-Act-Assert) 模式
- ✅ 测试边界条件和异常情况
- ✅ 使用 AssertJ 提高断言可读性
- ✅ 保持测试简单易懂

---

## 🔗 相关资源

- [JUnit 5 官方文档](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito 文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ 文档](https://assertj.github.io/doc/)

祝测试愉快！🚀
