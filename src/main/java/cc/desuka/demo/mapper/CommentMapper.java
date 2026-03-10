package cc.desuka.demo.mapper;

import cc.desuka.demo.dto.CommentResponse;
import cc.desuka.demo.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface CommentMapper {

    @Mapping(source = "task.id", target = "taskId")
    CommentResponse toResponse(Comment comment);

    List<CommentResponse> toResponseList(List<Comment> comments);
}
