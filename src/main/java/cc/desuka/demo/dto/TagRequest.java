package cc.desuka.demo.dto;

import cc.desuka.demo.model.Tag;
import cc.desuka.demo.validation.Unique;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Unique(entity = Tag.class, field = Tag.FIELD_NAME, message = "{tag.name.unique}")
public class TagRequest {

    private Long id;

    @NotBlank(message = "{tag.name.notBlank}")
    @Size(max = 50, message = "{tag.name.size}")
    private String name;
}
