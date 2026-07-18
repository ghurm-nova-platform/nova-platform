package ai.nova.platform.modelgateway.provider;

public class ProviderException extends Exception {

    private final String errorCode;
    private final ProviderFailureKind failureKind;

    public ProviderException(String errorCode, ProviderFailureKind failureKind, String message) {
        super(message);
        this.errorCode = errorCode;
        this.failureKind = failureKind;
    }

    public String errorCode() {
        return errorCode;
    }

    public ProviderFailureKind failureKind() {
        return failureKind;
    }
}
