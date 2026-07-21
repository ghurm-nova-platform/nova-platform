package ai.nova.platform.llm.entity;

public enum LlmModelStatus {
    REGISTERED,
    DOWNLOADING,
    INSTALLED,
    LOADING,
    READY,
    UNLOADING,
    STOPPED,
    ERROR,
    DISABLED
}
