package cc.desuka.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "checklist_items")
public class ChecklistItem {

  public static final String FIELD_ID = "id";
  public static final String FIELD_TEXT = "text";
  public static final String FIELD_CHECKED = "checked";
  public static final String FIELD_SORT_ORDER = "sortOrder";
  public static final String FIELD_TASK = "task";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Size(max = 200)
  @Column(nullable = false)
  private String text;

  @Column(nullable = false)
  private boolean checked = false;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder = 0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id", nullable = false)
  private Task task;

  public ChecklistItem() {
  }

  public ChecklistItem(String text, int sortOrder) {
    this.text = text;
    this.sortOrder = sortOrder;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public boolean isChecked() {
    return checked;
  }

  public void setChecked(boolean checked) {
    this.checked = checked;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  public Task getTask() {
    return task;
  }

  public void setTask(Task task) {
    this.task = task;
  }
}
