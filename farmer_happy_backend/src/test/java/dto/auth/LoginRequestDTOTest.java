package dto.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginRequestDTO 测试类
 * 
 * 测试登录请求DTO的功能：
 * - 字段验证
 * - Getter/Setter 方法
 * - 边界条件
 */
@DisplayName("登录请求DTO测试")
class LoginRequestDTOTest {

    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequestDTO();
    }

    @Test
    @DisplayName("测试电话号码 Getter 和 Setter")
    void testPhoneGetterSetter() {
        // Given
        String phone = "13800138000";

        // When
        loginRequest.setPhone(phone);

        // Then
        assertThat(loginRequest.getPhone())
            .isEqualTo(phone);
    }

    @Test
    @DisplayName("测试密码 Getter 和 Setter")
    void testPasswordGetterSetter() {
        // Given
        String password = "securePassword123";

        // When
        loginRequest.setPassword(password);

        // Then
        assertThat(loginRequest.getPassword())
            .isEqualTo(password);
    }

    @Test
    @DisplayName("测试用户类型 Getter 和 Setter")
    void testUserTypeGetterSetter() {
        // Given
        String userType = "farmer";

        // When
        loginRequest.setUserType(userType);

        // Then
        assertThat(loginRequest.getUserType())
            .isEqualTo(userType);
    }

    @Test
    @DisplayName("测试完整的登录请求对象")
    void testCompleteLoginRequest() {
        // Given
        String phone = "13800138000";
        String password = "testpass";
        String userType = "farmer";

        // When
        loginRequest.setPhone(phone);
        loginRequest.setPassword(password);
        loginRequest.setUserType(userType);

        // Then
        assertThat(loginRequest)
            .hasFieldOrPropertyWithValue("phone", phone)
            .hasFieldOrPropertyWithValue("password", password)
            .hasFieldOrPropertyWithValue("userType", userType);
    }

    @ParameterizedTest
    @DisplayName("测试有效的电话号码")
    @ValueSource(strings = {"13800138000", "13912345678", "15800000000", "18600000000"})
    void testValidPhones(String phone) {
        loginRequest.setPhone(phone);
        
        assertThat(loginRequest.getPhone())
            .isNotNull()
            .isNotEmpty()
            .isEqualTo(phone)
            .matches("^1[3-9]\\d{9}$");
    }

    @ParameterizedTest
    @DisplayName("测试不同的用户类型")
    @ValueSource(strings = {"farmer", "buyer", "expert", "bank", "admin"})
    void testDifferentUserTypes(String userType) {
        loginRequest.setUserType(userType);
        
        assertThat(loginRequest.getUserType())
            .isNotNull()
            .isEqualTo(userType);
    }

    @ParameterizedTest
    @DisplayName("测试空电话号码和null")
    @NullAndEmptySource
    void testInvalidPhones(String phone) {
        loginRequest.setPhone(phone);
        
        String retrievedPhone = loginRequest.getPhone();
        
        // 验证设置的是null或空字符串
        if (phone == null) {
            assertThat(retrievedPhone).isNull();
        } else {
            assertThat(retrievedPhone).isEmpty();
        }
    }

    @Test
    @DisplayName("测试创建登录请求并验证所有字段")
    void testLoginRequestValidation() {
        // Given
        String phone = "13800138000";
        String password = "ValidPass123!";
        String userType = "farmer";

        // When
        loginRequest.setPhone(phone);
        loginRequest.setPassword(password);
        loginRequest.setUserType(userType);

        // Then
        assertAll("登录请求验证",
            () -> assertNotNull(loginRequest.getPhone(), "电话号码不应为null"),
            () -> assertNotNull(loginRequest.getPassword(), "密码不应为null"),
            () -> assertNotNull(loginRequest.getUserType(), "用户类型不应为null"),
            () -> assertTrue(loginRequest.getPhone().length() > 0, "电话号码长度应大于0"),
            () -> assertTrue(loginRequest.getPassword().length() > 0, "密码长度应大于0")
        );
    }

    @Test
    @DisplayName("测试DTO字符串表示")
    void testToString() {
        // Given
        loginRequest.setPhone("13800138000");
        loginRequest.setPassword("password");
        loginRequest.setUserType("farmer");

        // When
        String result = loginRequest.toString();

        // Then
        assertThat(result)
            .isNotNull();
    }

    @Test
    @DisplayName("测试两个相同内容的DTO对象")
    void testEqualDTOs() {
        // Given
        LoginRequestDTO request1 = new LoginRequestDTO();
        request1.setPhone("13800138000");
        request1.setPassword("pass1");
        request1.setUserType("farmer");

        LoginRequestDTO request2 = new LoginRequestDTO();
        request2.setPhone("13800138000");
        request2.setPassword("pass1");
        request2.setUserType("farmer");

        // Then
        assertThat(request1.getPhone())
            .isEqualTo(request2.getPhone());
        assertThat(request1.getPassword())
            .isEqualTo(request2.getPassword());
        assertThat(request1.getUserType())
            .isEqualTo(request2.getUserType());
    }

    @Test
    @DisplayName("测试电话号码格式验证")
    void testPhoneFormatValidation() {
        // Given
        String validPhone = "13800138000";
        
        // When
        loginRequest.setPhone(validPhone);
        
        // Then
        assertThat(loginRequest.getPhone())
            .as("电话号码应该符合中国手机号格式")
            .matches("^1[3-9]\\d{9}$");
    }
}
