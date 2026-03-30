package cc.desuka.demo.mapper;

import cc.desuka.demo.dto.RecurringTaskTemplateRequest;
import cc.desuka.demo.dto.RecurringTaskTemplateResponse;
import cc.desuka.demo.model.RecurringTaskTemplate;
import cc.desuka.demo.model.Tag;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        uses = {TagMapper.class})
public interface RecurringTaskTemplateMapper {

    @Mapping(source = "assignee.id", target = "assigneeId")
    @Mapping(source = "assignee.name", target = "assigneeName")
    @Mapping(source = "createdBy.name", target = "createdByName")
    RecurringTaskTemplateResponse toResponse(RecurringTaskTemplate template);

    List<RecurringTaskTemplateResponse> toResponseList(List<RecurringTaskTemplate> templates);

    @Mapping(source = "assignee.id", target = "assigneeId")
    @Mapping(target = "tagIds", expression = "java(tagIds(template.getTags()))")
    RecurringTaskTemplateRequest toRequest(RecurringTaskTemplate template);

    default List<Long> tagIds(Set<Tag> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return tags.stream().map(Tag::getId).toList();
    }
}
