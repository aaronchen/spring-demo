package cc.desuka.demo.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class CommentResponse {

    private Long id;
    private String text;
    private UUID taskId;
    private UserResponse user;
    private LocalDateTime createdAt;
}
