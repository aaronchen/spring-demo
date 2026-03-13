package cc.desuka.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {

    private Long id;
    private String type;
    private String message;
    private String link;
    private boolean read;
    private LocalDateTime createdAt;
    private String actorName;
}
