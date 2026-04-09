package cc.desuka.demo.mapper;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.dto.PinnedItemResponse;
import cc.desuka.demo.model.PinnedItem;
import cc.desuka.demo.util.EntityTypes;
import java.util.List;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PinnedItemMapper {

    @Mapping(target = "href", expression = "java(resolveHref(pin, appRoutes))")
    @Mapping(target = "pinned", constant = "false")
    @Mapping(target = "titleOnly", constant = "false")
    @Mapping(target = "deleted", constant = "false")
    PinnedItemResponse toResponse(PinnedItem pin, @Context AppRoutesProperties appRoutes);

    List<PinnedItemResponse> toResponseList(
            List<PinnedItem> pins, @Context AppRoutesProperties appRoutes);

    default String resolveHref(PinnedItem pin, AppRoutesProperties appRoutes) {
        return EntityTypes.resolveHref(appRoutes, pin.getEntityType(), pin.getEntityId());
    }
}
