package cc.desuka.demo.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for parsing and rendering @mention tokens in comment text.
 *
 * Encoded format: @[Display Name](userId:3)
 * Rendered as: <span class="mention">@Display Name</span>
 */
@Component("mentionUtils")
public class MentionUtils {

    // Matches @[Display Name](userId:123)
    private static final Pattern MENTION_PATTERN =
        Pattern.compile("@\\[([^\\]]+)]\\(userId:(\\d+)\\)");

    /**
     * Extract all mentioned user IDs from text containing encoded mention tokens.
     */
    public static List<Long> extractMentionedUserIds(String text) {
        List<Long> ids = new ArrayList<>();
        if (text == null) return ids;
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            ids.add(Long.parseLong(matcher.group(2)));
        }
        return ids;
    }

    /**
     * Render encoded mention tokens as styled HTML spans.
     * Also escapes HTML in the surrounding text to prevent XSS.
     */
    public String renderHtml(String text) {
        if (text == null) return "";
        Matcher matcher = MENTION_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(escapeHtml(text.substring(lastEnd, matcher.start())));
            String displayName = escapeHtml(matcher.group(1));
            String userId = matcher.group(2);
            sb.append("<a href=\"/tasks?selectedUserId=").append(userId)
              .append("\" class=\"mention\">@").append(displayName).append("</a>");
            lastEnd = matcher.end();
        }
        sb.append(escapeHtml(text.substring(lastEnd)));
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
