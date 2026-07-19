package ai.nova.platform.approval.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class ApprovalAuthorizationService {

    public static final String APPROVAL_GATE_RUN = "APPROVAL_GATE_RUN";
    public static final String APPROVAL_GATE_READ = "APPROVAL_GATE_READ";
    public static final String APPROVAL_GATE_APPROVE = "APPROVAL_GATE_APPROVE";
    public static final String APPROVAL_GATE_REJECT = "APPROVAL_GATE_REJECT";
    public static final String APPROVAL_POLICY_READ = "APPROVAL_POLICY_READ";
    public static final String APPROVAL_POLICY_MANAGE = "APPROVAL_POLICY_MANAGE";

    public void require(AuthenticatedUser user, String permission) {
        if (user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (!user.hasPermission(permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Missing permission: " + permission);
        }
    }
}
