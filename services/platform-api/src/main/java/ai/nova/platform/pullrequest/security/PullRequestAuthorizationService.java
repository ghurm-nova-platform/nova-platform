package ai.nova.platform.pullrequest.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class PullRequestAuthorizationService {

    public static final String PR_RUN = "PR_RUN";
    public static final String PR_READ = "PR_READ";

    public void require(AuthenticatedUser user, String permission) {
        if (user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (!user.hasPermission(permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Missing permission: " + permission);
        }
    }
}
