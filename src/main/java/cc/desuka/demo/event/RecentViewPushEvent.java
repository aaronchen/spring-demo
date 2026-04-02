package cc.desuka.demo.event;

import cc.desuka.demo.dto.RecentViewResponse;

public record RecentViewPushEvent(String userEmail, RecentViewResponse payload) {}
