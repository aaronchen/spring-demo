package cc.desuka.demo.util;

import static org.assertj.core.api.Assertions.assertThat;

import cc.desuka.demo.config.AppRoutesProperties;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MentionUtilsTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final MentionUtils mentionUtils = new MentionUtils(new AppRoutesProperties());

    // ── extractMentionedUserIds ──────────────────────────────────────────

    @Test
    void extractMentionedUserIds_singleMention() {
        List<UUID> ids =
                MentionUtils.extractMentionedUserIds("Hello @[Alice](userId:" + ID_1 + ")");

        assertThat(ids).containsExactly(ID_1);
    }

    @Test
    void extractMentionedUserIds_multipleMentions() {
        List<UUID> ids =
                MentionUtils.extractMentionedUserIds(
                        "@[Alice](userId:"
                                + ID_1
                                + ") and @[Bob](userId:"
                                + ID_2
                                + ") please review");

        assertThat(ids).containsExactly(ID_1, ID_2);
    }

    @Test
    void extractMentionedUserIds_duplicateMentions_preservesAll() {
        List<UUID> ids =
                MentionUtils.extractMentionedUserIds(
                        "@[Alice](userId:" + ID_1 + ") hey @[Alice](userId:" + ID_1 + ")");

        assertThat(ids).containsExactly(ID_1, ID_1);
    }

    @Test
    void extractMentionedUserIds_noMentions() {
        List<UUID> ids = MentionUtils.extractMentionedUserIds("Plain text comment");

        assertThat(ids).isEmpty();
    }

    @Test
    void extractMentionedUserIds_nullText() {
        List<UUID> ids = MentionUtils.extractMentionedUserIds(null);

        assertThat(ids).isEmpty();
    }

    @Test
    void extractMentionedUserIds_malformedMention_ignored() {
        List<UUID> ids = MentionUtils.extractMentionedUserIds("@[Alice](user:1)");

        assertThat(ids).isEmpty();
    }

    // ── renderHtml ───────────────────────────────────────────────────────

    @Test
    void renderHtml_convertsMentionToLink() {
        String result = mentionUtils.renderHtml("Hey @[Alice](userId:" + ID_1 + ")!");

        assertThat(result)
                .isEqualTo(
                        "Hey <a href=\"/tasks?selectedUserId="
                                + ID_1
                                + "\" class=\"mention\">@Alice</a>!");
    }

    @Test
    void renderHtml_multipleMentions() {
        String result =
                mentionUtils.renderHtml(
                        "@[Alice](userId:" + ID_1 + ") and @[Bob](userId:" + ID_2 + ")");

        assertThat(result).contains("selectedUserId=" + ID_1);
        assertThat(result).contains("selectedUserId=" + ID_2);
    }

    @Test
    void renderHtml_escapesHtmlInSurroundingText() {
        String result =
                mentionUtils.renderHtml(
                        "<script>alert('xss')</script> @[Alice](userId:" + ID_1 + ")");

        assertThat(result).doesNotContain("<script>");
        assertThat(result).contains("&lt;script&gt;");
    }

    @Test
    void renderHtml_escapesHtmlInDisplayName() {
        String result = mentionUtils.renderHtml("@[<b>Evil</b>](userId:" + ID_1 + ")");

        assertThat(result).doesNotContain("<b>");
        assertThat(result).contains("&lt;b&gt;Evil&lt;/b&gt;");
    }

    @Test
    void renderHtml_nullText_returnsEmpty() {
        assertThat(mentionUtils.renderHtml(null)).isEmpty();
    }

    @Test
    void renderHtml_noMentions_returnsEscapedText() {
        String result = mentionUtils.renderHtml("Just plain text & stuff");

        assertThat(result).isEqualTo("Just plain text &amp; stuff");
    }
}
