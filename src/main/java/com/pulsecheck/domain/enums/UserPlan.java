package com.pulsecheck.domain.enums;

public enum UserPlan {
    FREE(5, 300, 1),
    PRO(50, 60, 3),
    AGENCY(200, 30, 10),
    ENTERPRISE(Integer.MAX_VALUE, 15, Integer.MAX_VALUE);

    public final int maxMonitors;
    public final int minIntervalSeconds;
    public final int maxStatusPages;

    UserPlan(int maxMonitors, int minIntervalSeconds, int maxStatusPages) {
        this.maxMonitors = maxMonitors;
        this.minIntervalSeconds = minIntervalSeconds;
        this.maxStatusPages = maxStatusPages;
    }
}
