package cc.desuka.demo.mapper;

import cc.desuka.demo.dto.UserRequest;
import cc.desuka.demo.dto.UserResponse;
import cc.desuka.demo.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    @Mapping(target = User.FIELD_ID, ignore = true)
    @Mapping(target = User.FIELD_PASSWORD, ignore = true)
    @Mapping(target = User.FIELD_ROLE, ignore = true)
    @Mapping(target = User.FIELD_TASKS, ignore = true)
    User toEntity(UserRequest request);
}
