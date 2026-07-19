package ai.nova.platform.repair.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class RepairAuthorizationService {

    public static final String REPAIR_RUN = "REPAIR_RUN";
    public static final String REPAIR_READ = "REPAIR_READ";

    public void require(AuthenticatedUser user, String permission) {
        if (user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (!user.hasPermission(permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Missing permission: " + permission);
        }
    }
}
