package cc.desuka.demo.dto;

import cc.desuka.demo.model.User;
import cc.desuka.demo.validation.Unique;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Unique(entity = User.class, field = User.FIELD_EMAIL, message = "{user.email.unique}")
public class ProfileRequest {

    // Set to current user's ID — used by @Unique to exclude self
    private Long id;

    @NotBlank(message = "{user.name.notBlank}")
    @Size(max = 100, message = "{user.name.size}")
    private String name;

    @NotBlank(message = "{user.email.notBlank}")
    @Size(max = 150, message = "{user.email.size}")
    @Email(message = "{user.email.invalid}")
    private String email;
}
