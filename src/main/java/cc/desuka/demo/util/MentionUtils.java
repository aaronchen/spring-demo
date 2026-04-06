package cc.desuka.demo.util;

import cc.desuka.demo.config.AppRoutesProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Utilities for parsing and rendering @mention tokens in comment text.
 *
 * <p>Encoded format: {@code @[Display Name](userId:<uuid>)} Rendered as: {@code <a
 * class="mention">@Display Name</a>}
 */
@Component("mentionUtils")
public class MentionUtils {

    // Matches @[Display Name](userId:<uuid>)
    private static final Pattern MENTION_PATTERN =
            Pattern.compile("@\\[([^\\]]+)]\\(userId:([0-9a-fA-F\\-]+)\\)");

    private final AppRoutesProperties appRoutes;

    public MentionUtils(AppRoutesProperties appRoutes) {
        this.appRoutes = appRoutes;
    }

    /** Decode encoded mention tokens to plain text: {@code @[Name](userId:N)} → {@code @Name}. */
    public static String decodePlainText(String text) {
        if (text == null) return "";
        return MENTION_PATTERN.matcher(text).replaceAll("@$1");
    }

    /** Extract all mentioned user IDs from text containing encoded mention tokens. */
    public static List<UUID> extractMentionedUserIds(String text) {
        List<UUID> ids = new ArrayList<>();
        if (text == null) return ids;
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            ids.add(UUID.fromString(matcher.group(2)));
        }
        return ids;
    }

    /**
     * Render encoded mention tokens as styled HTML spans. Also escapes HTML in the surrounding text
     * to prevent XSS.
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
            String href = appRoutes.getTasks().resolve(Map.of(), Map.of("selectedUserId", userId));
            sb.append("<a href=\"")
                    .append(escapeHtml(href))
                    .append("\" class=\"mention\">@")
                    .append(displayName)
                    .append("</a>");
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
