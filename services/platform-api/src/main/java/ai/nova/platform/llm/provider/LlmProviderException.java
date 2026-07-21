package ai.nova.platform.llm.provider;

public class LlmProviderException extends RuntimeException {

    private final String errorCode;
    private final boolean retryable;

    public LlmProviderException(String errorCode, String message, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
