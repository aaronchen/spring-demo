package cc.desuka.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "{profile.password.current.notBlank}")
    private String currentPassword;

    @NotBlank(message = "{user.password.notBlank}")
    @Size(min = 8, max = 72, message = "{user.password.size}")
    private String newPassword;

    @NotBlank(message = "{profile.password.confirm.notBlank}")
    private String confirmPassword;
}
