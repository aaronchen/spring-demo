package cc.desuka.demo.util;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvWriter {

    public static <T> void write(
            HttpServletResponse response,
            String filename,
            String[] headers,
            List<T> rows,
            Function<T, String[]> rowMapper)
            throws IOException {
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.println(formatRow(headers));
            for (T row : rows) {
                writer.println(formatRow(rowMapper.apply(row)));
            }
        }
    }

    private static String formatRow(String[] fields) {
        return Stream.of(fields).map(CsvWriter::escape).collect(Collectors.joining(","));
    }

    private static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
