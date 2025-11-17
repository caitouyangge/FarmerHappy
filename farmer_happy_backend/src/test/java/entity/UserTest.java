package entity;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * User 实体类测试
 * 
 * 测试实体类的基本功能：
 * - Getter/Setter 方法
 * - 构造函数
 * - 数据验证
 */
@DisplayName("用户实体测试")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    @DisplayName("测试用户UID的 Getter 和 Setter")
    void testUserIdGetterSetter() {
        // Given
        String expectedUid = "test-uid-123";

        // When
        user.setUid(expectedUid);

        // Then
        assertThat(user.getUid())
            .as("用户UID应该正确设置")
            .isEqualTo(expectedUid);
    }

    @Test
    @DisplayName("测试昵称的 Getter 和 Setter")
    void testNicknameGetterSetter() {
        // Given
        String expectedNickname = "张三农民";

        // When
        user.setNickname(expectedNickname);

        // Then
        assertThat(user.getNickname())
            .as("昵称应该正确设置")
            .isEqualTo(expectedNickname);
    }

    @Test
    @DisplayName("测试密码的 Getter 和 Setter")
    void testPasswordGetterSetter() {
        // Given
        String expectedPassword = "securePassword123";

        // When
        user.setPassword(expectedPassword);

        // Then
        assertThat(user.getPassword())
            .as("密码应该正确设置")
            .isEqualTo(expectedPassword);
    }

    @Test
    @DisplayName("测试电话号码的 Getter 和 Setter")
    void testPhoneGetterSetter() {
        // Given
        String expectedPhone = "13800138000";

        // When
        user.setPhone(expectedPhone);

        // Then
        assertThat(user.getPhone())
            .as("电话号码应该正确设置")
            .isEqualTo(expectedPhone);
    }

    @ParameterizedTest(name = "测试 {index}: 电话 = {0}, 描述 = {1}")
    @DisplayName("参数化测试 - 不同电话格式")
    @CsvSource({
        "13800138000, 标准手机号",
        "13912345678, 标准手机号",
        "15800000000, 标准手机号",
        "18600000000, 标准手机号"
    })
    void testDifferentPhoneFormats(String phone, String description) {
        user.setPhone(phone);
        assertThat(user.getPhone())
            .as("电话应该是 %s", description)
            .isEqualTo(phone);
    }

    @Test
    @DisplayName("测试用户对象的完整设置")
    void testCompleteUserSetup() {
        // Given
        String uid = "uid-100";
        String nickname = "测试农民";
        String password = "TestPass123!";
        String phone = "13800138000";

        // When
        user.setUid(uid);
        user.setNickname(nickname);
        user.setPassword(password);
        user.setPhone(phone);

        // Then - 使用 AssertJ 的多重断言
        assertThat(user)
            .as("用户对象应该正确设置所有字段")
            .hasFieldOrPropertyWithValue("uid", uid)
            .hasFieldOrPropertyWithValue("nickname", nickname)
            .hasFieldOrPropertyWithValue("password", password)
            .hasFieldOrPropertyWithValue("phone", phone);
    }

    @Test
    @DisplayName("测试新创建的用户对象初始状态")
    void testNewUserInitialState() {
        User newUser = new User();
        
        assertThat(newUser)
            .as("新用户对象应该有默认初始值")
            .isNotNull();
        
        // 新用户应该有自动生成的 UID
        assertThat(newUser.getUid())
            .as("新用户应该有自动生成的UID")
            .isNotNull()
            .isNotEmpty();
        
        // 新用户应该有创建时间
        assertThat(newUser.getCreatedAt())
            .as("新用户应该有创建时间")
            .isNotNull();
        
        // 新用户应该有更新时间
        assertThat(newUser.getUpdatedAt())
            .as("新用户应该有更新时间")
            .isNotNull();
    }

    @Test
    @DisplayName("测试带参数的构造函数")
    void testParameterizedConstructor() {
        // Given
        String password = "TestPassword123!";
        String nickname = "测试用户";
        String phone = "13800138000";

        // When
        User newUser = new User(password, nickname, phone);

        // Then
        assertThat(newUser)
            .isNotNull();
        
        assertThat(newUser.getPassword())
            .isEqualTo(password);
        
        assertThat(newUser.getNickname())
            .isEqualTo(nickname);
        
        assertThat(newUser.getPhone())
            .isEqualTo(phone);
        
        // 应该有自动生成的 UID 和时间戳
        assertThat(newUser.getUid())
            .isNotNull()
            .isNotEmpty();
        
        assertThat(newUser.getCreatedAt())
            .isNotNull();
    }

    @Test
    @DisplayName("测试昵称字段不为空")
    void testNicknameNotNull() {
        user.setNickname("有效昵称");
        
        assertThat(user.getNickname())
            .as("昵称不应该为null")
            .isNotNull()
            .isNotEmpty()
            .isNotBlank();
    }

    @Test
    @DisplayName("测试电话号码格式验证")
    void testPhoneValidation() {
        // Given - 有效的中国手机号
        String validPhone = "13800138000";
        
        // When
        user.setPhone(validPhone);
        
        // Then
        assertThat(user.getPhone())
            .as("应该接受有效的手机号")
            .matches("^1[3-9]\\d{9}$");
    }

    @Test
    @DisplayName("测试创建时间和更新时间")
    void testTimestamps() {
        // Given
        User newUser = new User();
        
        // Then
        assertThat(newUser.getCreatedAt())
            .as("创建时间应该被自动设置")
            .isNotNull()
            .isBeforeOrEqualTo(java.time.LocalDateTime.now());
        
        assertThat(newUser.getUpdatedAt())
            .as("更新时间应该被自动设置")
            .isNotNull()
            .isBeforeOrEqualTo(java.time.LocalDateTime.now());
    }

    @Test
    @DisplayName("测试用户对象字符串表示")
    void testUserToString() {
        // Given
        user.setUid("uid-1");
        user.setNickname("testuser");
        user.setPhone("13800138000");

        // When
        String userString = user.toString();

        // Then
        assertThat(userString)
            .as("toString 方法应该返回有意义的字符串")
            .isNotNull();
    }
}
