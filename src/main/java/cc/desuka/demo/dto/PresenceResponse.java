package cc.desuka.demo.dto;

import java.util.List;

public record PresenceResponse(List<String> users, int count) {}
