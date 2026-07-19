package ai.nova.platform.approval.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.approval-gate")
public class ApprovalGateProperties {

    private boolean enabled = false;
    private int defaultRequiredHumanApprovals = 1;
    private int defaultDecisionValidityMinutes = 1440;
    private boolean requireDistinctApprovers = true;
    private boolean prohibitAuthorApproval = true;
    private boolean invalidateOnNewPatch = true;
    private boolean invalidateOnNewCommit = true;
    private boolean invalidateOnNewCiObservation = true;
    private boolean invalidateOnPrHeadChange = true;
    private boolean failClosed = true;
    private int maximumCommentLength = 2000;
    private int maxRepairAttempts = 5;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultRequiredHumanApprovals() {
        return defaultRequiredHumanApprovals;
    }

    public void setDefaultRequiredHumanApprovals(int defaultRequiredHumanApprovals) {
        this.defaultRequiredHumanApprovals = defaultRequiredHumanApprovals;
    }

    public int getDefaultDecisionValidityMinutes() {
        return defaultDecisionValidityMinutes;
    }

    public void setDefaultDecisionValidityMinutes(int defaultDecisionValidityMinutes) {
        this.defaultDecisionValidityMinutes = defaultDecisionValidityMinutes;
    }

    public boolean isRequireDistinctApprovers() {
        return requireDistinctApprovers;
    }

    public void setRequireDistinctApprovers(boolean requireDistinctApprovers) {
        this.requireDistinctApprovers = requireDistinctApprovers;
    }

    public boolean isProhibitAuthorApproval() {
        return prohibitAuthorApproval;
    }

    public void setProhibitAuthorApproval(boolean prohibitAuthorApproval) {
        this.prohibitAuthorApproval = prohibitAuthorApproval;
    }

    public boolean isInvalidateOnNewPatch() {
        return invalidateOnNewPatch;
    }

    public void setInvalidateOnNewPatch(boolean invalidateOnNewPatch) {
        this.invalidateOnNewPatch = invalidateOnNewPatch;
    }

    public boolean isInvalidateOnNewCommit() {
        return invalidateOnNewCommit;
    }

    public void setInvalidateOnNewCommit(boolean invalidateOnNewCommit) {
        this.invalidateOnNewCommit = invalidateOnNewCommit;
    }

    public boolean isInvalidateOnNewCiObservation() {
        return invalidateOnNewCiObservation;
    }

    public void setInvalidateOnNewCiObservation(boolean invalidateOnNewCiObservation) {
        this.invalidateOnNewCiObservation = invalidateOnNewCiObservation;
    }

    public boolean isInvalidateOnPrHeadChange() {
        return invalidateOnPrHeadChange;
    }

    public void setInvalidateOnPrHeadChange(boolean invalidateOnPrHeadChange) {
        this.invalidateOnPrHeadChange = invalidateOnPrHeadChange;
    }

    public boolean isFailClosed() {
        return failClosed;
    }

    public void setFailClosed(boolean failClosed) {
        this.failClosed = failClosed;
    }

    public int getMaximumCommentLength() {
        return maximumCommentLength;
    }

    public void setMaximumCommentLength(int maximumCommentLength) {
        this.maximumCommentLength = maximumCommentLength;
    }

    public int getMaxRepairAttempts() {
        return maxRepairAttempts;
    }

    public void setMaxRepairAttempts(int maxRepairAttempts) {
        this.maxRepairAttempts = maxRepairAttempts;
    }
}
