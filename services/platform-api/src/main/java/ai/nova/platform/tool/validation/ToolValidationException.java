package ai.nova.platform.tool.validation;

public class ToolValidationException extends RuntimeException {

    private final String code;

    public ToolValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
