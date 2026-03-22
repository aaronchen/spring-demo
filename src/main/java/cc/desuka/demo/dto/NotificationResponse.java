package cc.desuka.demo.dto;

import java.time.LocalDateTime;
import lombok.Data;

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
