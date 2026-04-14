package cc.desuka.demo.dto;

import java.util.UUID;

/** Lightweight task representation (id, title, status) for search results. */
public record TaskItem(UUID id, String title, String status) {}
