package cc.desuka.demo.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CommentResponse {

    private Long id;
    private String text;
    private Long taskId;
    private UserResponse user;
    private LocalDateTime createdAt;
}
