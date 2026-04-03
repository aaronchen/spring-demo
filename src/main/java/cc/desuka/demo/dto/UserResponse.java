package cc.desuka.demo.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class UserResponse {

    private UUID id;
    private String name;
    private String email;
}
