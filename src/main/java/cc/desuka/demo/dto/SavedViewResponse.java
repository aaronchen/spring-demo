package cc.desuka.demo.dto;

import cc.desuka.demo.model.SavedView;

public record SavedViewResponse(Long id, String name, String filters) {

    public static SavedViewResponse fromEntity(SavedView view) {
        return new SavedViewResponse(view.getId(), view.getName(), view.getFilters());
    }
}
