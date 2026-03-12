package cc.desuka.demo.dto;

import cc.desuka.demo.model.User;
import cc.desuka.demo.validation.Unique;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Unique(entity = User.class, field = User.FIELD_EMAIL, message = "{user.email.unique}")
public class UserRequest {

    @NotBlank(message = "{user.name.notBlank}")
    @Size(max = 100, message = "{user.name.size}")
    private String name;

    @NotBlank(message = "{user.email.notBlank}")
    @Size(max = 150, message = "{user.email.size}")
    private String email;
}
