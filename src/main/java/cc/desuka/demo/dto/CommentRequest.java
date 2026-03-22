package cc.desuka.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequest {

    @NotBlank(message = "{comment.text.notBlank}")
    @Size(max = 500, message = "{comment.text.size}")
    private String text;
}
