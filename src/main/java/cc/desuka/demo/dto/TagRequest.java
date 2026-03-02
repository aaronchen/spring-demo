package cc.desuka.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagRequest {

    @NotBlank(message = "{tag.name.notBlank}")
    @Size(max = 50, message = "{tag.name.size}")
    private String name;
}
