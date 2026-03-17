package cc.desuka.demo.dto;

import cc.desuka.demo.model.Task;

import java.time.LocalDate;
import java.util.List;

public record CalendarDay(LocalDate date, boolean currentMonth, boolean today, List<Task> tasks) {}
