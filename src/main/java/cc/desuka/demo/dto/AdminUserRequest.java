package cc.desuka.demo.dto;

import cc.desuka.demo.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserRequest {

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

    @NotNull
    private Role role = Role.USER;
}
