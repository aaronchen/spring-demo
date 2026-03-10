package cc.desuka.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponse {

    private Long id;
    private String text;
    private Long taskId;
    private UserResponse user;
    private LocalDateTime createdAt;
}
