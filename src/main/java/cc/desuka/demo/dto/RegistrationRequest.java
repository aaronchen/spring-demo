package cc.desuka.demo.dto;

import cc.desuka.demo.model.User;
import cc.desuka.demo.validation.Unique;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Unique(entity = User.class, field = User.FIELD_EMAIL, message = "{user.email.unique}")
public class RegistrationRequest {

    @NotBlank(message = "{user.name.notBlank}")
    @Size(max = 100, message = "{user.name.size}")
    private String name;

    @NotBlank(message = "{user.email.notBlank}")
    @Size(max = 150, message = "{user.email.size}")
    @Email(message = "{user.email.invalid}")
    private String email;

    @NotBlank(message = "{user.password.notBlank}")
    @Size(min = 8, max = 72, message = "{user.password.size}")
    private String password;

    // Validated programmatically against password in the controller —
    // no annotations here because the check is cross-field, not single-field.
    private String confirmPassword;
}
