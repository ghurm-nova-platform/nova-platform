package ai.nova.platform.planner.entity;

/**
 * Task classification vocabulary for planner output. Maps onto orchestration TaskType where possible;
 * specialized roles remain metadata until specialized agents exist.
 */
public enum PlannerTaskClassification {
    RESEARCH,
    CODING,
    TESTING,
    REVIEW,
    DOCUMENTATION,
    HUMAN_APPROVAL,
    TRANSFORMATION,
    AGGREGATION,
    PLANNING,
    SECURITY,
    DEVOPS,
    DATABASE,
    UI,
    BACKEND,
    FRONTEND,
    ARCHITECTURE,
    OTHER
}
