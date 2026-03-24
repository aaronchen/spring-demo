package cc.desuka.demo.dto;

import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Typed representation of a saved view's data. Wraps a filter query object plus view preferences
 * (view mode, sort order).
 *
 * <p>The {@code type} field discriminates which kind of query this view represents. Currently only
 * {@code "task"} is supported; future feature areas can add their own types.
 */
public class SavedViewData {

    public static final String TYPE_TASK = "task";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String type = TYPE_TASK;
    private TaskListQuery query;
    private String view;
    private List<SortField> sort;

    public SavedViewData() {}

    public SavedViewData(String type, TaskListQuery query, String view, List<SortField> sort) {
        this.type = type;
        this.query = query;
        this.view = view;
        this.sort = sort;
    }

    /** Creates a task-type saved view data. */
    public static SavedViewData ofTask(TaskListQuery query, String view, List<SortField> sort) {
        return new SavedViewData(TYPE_TASK, query, view, sort);
    }

    /** Serializes to JSON for storage in the entity column. */
    public String toJson() {
        return MAPPER.writeValueAsString(this);
    }

    /** Deserializes from the entity's JSON column. */
    public static SavedViewData fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, SavedViewData.class);
        } catch (JacksonException e) {
            return null;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public TaskListQuery getQuery() {
        return query;
    }

    public void setQuery(TaskListQuery query) {
        this.query = query;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public List<SortField> getSort() {
        return sort;
    }

    public void setSort(List<SortField> sort) {
        this.sort = sort;
    }

    /** A single sort field with direction. */
    public record SortField(String field, String direction) {}
}
