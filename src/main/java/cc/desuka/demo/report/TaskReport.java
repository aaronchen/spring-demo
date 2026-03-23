package cc.desuka.demo.report;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.util.CsvWriter;
import cc.desuka.demo.util.Messages;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TaskReport {

    private final Messages messages;

    public TaskReport(Messages messages) {
        this.messages = messages;
    }

    public void exportCsv(HttpServletResponse response, String filename, List<Task> tasks)
            throws IOException {
        String[] headers = {
            messages.get("task.field.title"),
            messages.get("task.field.status"),
            messages.get("task.field.priority"),
            messages.get("task.field.startDate"),
            messages.get("task.field.dueDate"),
            messages.get("task.field.completedAt"),
            messages.get("task.field.user"),
            messages.get("task.field.tags"),
            messages.get("task.field.createdAt"),
            messages.get("task.field.updatedAt"),
        };
        CsvWriter.write(
                response,
                filename,
                headers,
                tasks,
                task ->
                        new String[] {
                            task.getTitle(),
                            task.getStatus() != null ? messages.get(task.getStatus()) : "",
                            task.getPriority() != null ? messages.get(task.getPriority()) : "",
                            task.getStartDate() != null ? task.getStartDate().toString() : "",
                            task.getDueDate() != null ? task.getDueDate().toString() : "",
                            task.getCompletedAt() != null
                                    ? task.getCompletedAt().toLocalDate().toString()
                                    : "",
                            task.getUser() != null ? task.getUser().getName() : "",
                            task.getTags() != null
                                    ? task.getTags().stream()
                                            .map(t -> t.getName())
                                            .collect(Collectors.joining("; "))
                                    : "",
                            task.getCreatedAt() != null
                                    ? task.getCreatedAt().toLocalDate().toString()
                                    : "",
                            task.getUpdatedAt() != null
                                    ? task.getUpdatedAt().toLocalDate().toString()
                                    : ""
                        });
    }
}
