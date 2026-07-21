package ai.nova.platform.llm.error;

public final class LlmErrorCodes {

    public static final String PERMISSION_DENIED = "LLM_PERMISSION_DENIED";
    public static final String NOT_FOUND = "LLM_NOT_FOUND";
    public static final String CONFLICT = "LLM_CONFLICT";
    public static final String INVALID_STATE = "LLM_INVALID_STATE";
    public static final String PROVIDER_ERROR = "LLM_PROVIDER_ERROR";
    public static final String PROVIDER_UNAVAILABLE = "LLM_PROVIDER_UNAVAILABLE";
    public static final String CANCELLED = "LLM_CANCELLED";
    public static final String DISABLED = "LLM_DISABLED";

    private LlmErrorCodes() {
    }
}
