package ai.nova.platform.repair.entity;

public enum RepairInputSource {
    COMPILE(1),
    TEST(2),
    CI(3),
    STATIC_ANALYSIS(4),
    REVIEW(5),
    FORMATTING(6),
    DEPENDENCY(7),
    COVERAGE(8);

    private final int priority;

    RepairInputSource(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
