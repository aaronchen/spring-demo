package cc.desuka.demo.model;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

public enum TaskFilter {
  ALL, COMPLETED, INCOMPLETE;

  public static TaskFilter from(String value) {
    if (value == null) return ALL;
    try {
      return valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      return ALL;
    }
  }

  /** Auto-registered by Spring Boot — binds ?filter=completed → TaskFilter.COMPLETED */
  @Component
  public static class StringConverter implements Converter<String, TaskFilter> {
    @Override
    public TaskFilter convert(String source) {
      return TaskFilter.from(source);
    }
  }
}
