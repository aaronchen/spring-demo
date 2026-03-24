package cc.desuka.demo.dto;

import cc.desuka.demo.model.SavedView;

public record SavedViewResponse(Long id, String name, SavedViewData data) {

    public static SavedViewResponse fromEntity(SavedView view) {
        return new SavedViewResponse(
                view.getId(), view.getName(), SavedViewData.fromJson(view.getFilters()));
    }
}
