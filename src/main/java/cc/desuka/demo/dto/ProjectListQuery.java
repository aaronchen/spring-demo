package cc.desuka.demo.dto;

public class ProjectListQuery {

    private String sort = "name";
    private boolean showArchived;

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public boolean isShowArchived() {
        return showArchived;
    }

    public void setShowArchived(boolean showArchived) {
        this.showArchived = showArchived;
    }
}
