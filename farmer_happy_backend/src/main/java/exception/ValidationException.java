package exception;

import java.util.List;
import java.util.Map;

/**
 * 参数验证异常类
 * 用于传递详细的验证错误信息
 */
public class ValidationException extends IllegalArgumentException {
    private final List<Map<String, String>> errors;
    
    public ValidationException(String message, List<Map<String, String>> errors) {
        super(message);
        this.errors = errors;
    }
    
    public List<Map<String, String>> getErrors() {
        return errors;
    }
}

