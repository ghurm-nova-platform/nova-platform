package ai.nova.platform.llm.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.llm.error.LlmErrorCodes;
import ai.nova.platform.llm.permission.LlmPermissionCodes;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class LlmAuthorizationService {

    public void requireRead(AuthenticatedUser user) {
        require(user, LlmPermissionCodes.LLM_READ);
    }

    public void requireInfer(AuthenticatedUser user) {
        requireAny(user, LlmPermissionCodes.LLM_INFER, LlmPermissionCodes.LLM_ADMIN);
    }

    public void requireAdmin(AuthenticatedUser user) {
        require(user, LlmPermissionCodes.LLM_ADMIN);
    }

    public void requireModelAdmin(AuthenticatedUser user) {
        requireAny(user, LlmPermissionCodes.LLM_MODEL_ADMIN, LlmPermissionCodes.LLM_ADMIN);
    }

    public void requirePromptAdmin(AuthenticatedUser user) {
        requireAny(user, LlmPermissionCodes.LLM_PROMPT_ADMIN, LlmPermissionCodes.LLM_ADMIN);
    }

    public void require(AuthenticatedUser user, String permission) {
        if (user != null && user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (user == null || !user.hasPermission(permission)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, LlmErrorCodes.PERMISSION_DENIED, "Missing permission: " + permission);
        }
    }

    private void requireAny(AuthenticatedUser user, String... permissions) {
        if (user != null && user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (user != null) {
            for (String permission : permissions) {
                if (user.hasPermission(permission)) {
                    return;
                }
            }
        }
        throw new ApiException(
                HttpStatus.FORBIDDEN,
                LlmErrorCodes.PERMISSION_DENIED,
                "Missing one of permissions: " + String.join(", ", permissions));
    }
}
