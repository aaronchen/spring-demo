package cc.desuka.demo.event;

import cc.desuka.demo.dto.PinnedItemResponse;

/** Carries a pinned-item update to be pushed to a specific user via WebSocket. */
public record PinnedItemPushEvent(String userEmail, PinnedItemResponse payload) {}
