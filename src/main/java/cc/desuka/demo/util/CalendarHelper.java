package cc.desuka.demo.util;

import cc.desuka.demo.dto.CalendarDay;
import cc.desuka.demo.dto.TaskSearchCriteria;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.service.TaskQueryService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class CalendarHelper {

    private final TaskQueryService taskQueryService;

    public CalendarHelper(TaskQueryService taskQueryService) {
        this.taskQueryService = taskQueryService;
    }

    public record CalendarResult(List<List<CalendarDay>> weeks, long undatedCount) {}

    public CalendarResult buildCalendarWeeks(YearMonth month, TaskSearchCriteria criteria) {
        LocalDate firstOfMonth = month.atDay(1);
        LocalDate lastOfMonth = month.atEndOfMonth();
        LocalDate gridStart = firstOfMonth.with(DayOfWeek.MONDAY);
        LocalDate gridEnd = lastOfMonth.with(DayOfWeek.SUNDAY);

        criteria.setDueDateFrom(gridStart);
        criteria.setDueDateTo(gridEnd);
        Pageable unpaged = Pageable.unpaged(Sort.by(Sort.Direction.ASC, Task.FIELD_DUE_DATE));
        List<Task> tasks = taskQueryService.searchTasks(criteria, unpaged).getContent();

        Map<LocalDate, List<Task>> tasksByDate = new LinkedHashMap<>();
        long undatedCount = 0;
        for (Task task : tasks) {
            LocalDate date = (task.getDueDate() != null) ? task.getDueDate() : task.getStartDate();
            if (date == null) {
                undatedCount++;
                continue;
            }
            tasksByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(task);
        }

        LocalDate today = LocalDate.now();
        List<List<CalendarDay>> weeks = new ArrayList<>();
        LocalDate cursor = gridStart;
        while (!cursor.isAfter(gridEnd)) {
            List<CalendarDay> week = new ArrayList<>(7);
            for (int i = 0; i < 7; i++) {
                week.add(
                        new CalendarDay(
                                cursor,
                                !cursor.isBefore(firstOfMonth) && !cursor.isAfter(lastOfMonth),
                                cursor.equals(today),
                                tasksByDate.getOrDefault(cursor, List.of())));
                cursor = cursor.plusDays(1);
            }
            weeks.add(week);
        }
        return new CalendarResult(weeks, undatedCount);
    }
}
