package ai.nova.platform.identity.error;

public final class IdentityErrorCodes {

    public static final String IDENTITY_PROVIDER_NOT_FOUND = "IDENTITY_PROVIDER_NOT_FOUND";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String IDENTITY_USER_NOT_FOUND = USER_NOT_FOUND;
    public static final String GROUP_NOT_FOUND = "GROUP_NOT_FOUND";
    public static final String ROLE_NOT_FOUND = "ROLE_NOT_FOUND";
    public static final String PERMISSION_NOT_FOUND = "PERMISSION_NOT_FOUND";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String PASSWORD_EXPIRED = "PASSWORD_EXPIRED";
    public static final String MFA_REQUIRED = "MFA_REQUIRED";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String TOKEN_INVALID = "TOKEN_INVALID";
    public static final String SESSION_NOT_FOUND = "SESSION_NOT_FOUND";
    public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
    public static final String API_TOKEN_NOT_FOUND = "API_TOKEN_NOT_FOUND";
    public static final String SERVICE_ACCOUNT_NOT_FOUND = "SERVICE_ACCOUNT_NOT_FOUND";

    private IdentityErrorCodes() {
    }
}
