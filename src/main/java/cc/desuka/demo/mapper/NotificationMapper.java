package cc.desuka.demo.mapper;

import cc.desuka.demo.dto.NotificationResponse;
import cc.desuka.demo.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = Notification.FIELD_ACTOR + ".name", target = "actorName")
    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);
}
