package ai.nova.platform.prreview.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class PullRequestReviewAuthorizationService {

    public static final String PR_REVIEW_READ = "PR_REVIEW_READ";
    public static final String PR_REVIEW_RUN = "PR_REVIEW_RUN";
    public static final String PR_REVIEW_ADMIN = "PR_REVIEW_ADMIN";

    public void requireRead(AuthenticatedUser user) {
        require(user, PR_REVIEW_READ);
    }

    public void requireRun(AuthenticatedUser user) {
        require(user, PR_REVIEW_RUN);
    }

    public void requireExport(AuthenticatedUser user) {
        require(user, PR_REVIEW_READ);
    }

    public void requireAdmin(AuthenticatedUser user) {
        require(user, PR_REVIEW_ADMIN);
    }

    public void require(AuthenticatedUser user, String permission) {
        if (user != null && user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (user == null || !user.hasPermission(permission)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "PR_REVIEW_PERMISSION_DENIED", "Missing permission: " + permission);
        }
    }
}
