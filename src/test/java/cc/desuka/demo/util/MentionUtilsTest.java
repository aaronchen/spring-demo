package cc.desuka.demo.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MentionUtilsTest {

    private final MentionUtils mentionUtils = new MentionUtils();

    // ── extractMentionedUserIds ──────────────────────────────────────────

    @Test
    void extractMentionedUserIds_singleMention() {
        List<Long> ids = MentionUtils.extractMentionedUserIds("Hello @[Alice](userId:1)");

        assertThat(ids).containsExactly(1L);
    }

    @Test
    void extractMentionedUserIds_multipleMentions() {
        List<Long> ids = MentionUtils.extractMentionedUserIds(
                "@[Alice](userId:1) and @[Bob](userId:2) please review");

        assertThat(ids).containsExactly(1L, 2L);
    }

    @Test
    void extractMentionedUserIds_duplicateMentions_preservesAll() {
        List<Long> ids = MentionUtils.extractMentionedUserIds(
                "@[Alice](userId:1) hey @[Alice](userId:1)");

        assertThat(ids).containsExactly(1L, 1L);
    }

    @Test
    void extractMentionedUserIds_noMentions() {
        List<Long> ids = MentionUtils.extractMentionedUserIds("Plain text comment");

        assertThat(ids).isEmpty();
    }

    @Test
    void extractMentionedUserIds_nullText() {
        List<Long> ids = MentionUtils.extractMentionedUserIds(null);

        assertThat(ids).isEmpty();
    }

    @Test
    void extractMentionedUserIds_malformedMention_ignored() {
        List<Long> ids = MentionUtils.extractMentionedUserIds("@[Alice](user:1)");

        assertThat(ids).isEmpty();
    }

    // ── renderHtml ───────────────────────────────────────────────────────

    @Test
    void renderHtml_convertsMentionToLink() {
        String result = mentionUtils.renderHtml("Hey @[Alice](userId:1)!");

        assertThat(result).isEqualTo(
                "Hey <a href=\"/tasks?selectedUserId=1\" class=\"mention\">@Alice</a>!");
    }

    @Test
    void renderHtml_multipleMentions() {
        String result = mentionUtils.renderHtml("@[Alice](userId:1) and @[Bob](userId:2)");

        assertThat(result).contains("selectedUserId=1");
        assertThat(result).contains("selectedUserId=2");
    }

    @Test
    void renderHtml_escapesHtmlInSurroundingText() {
        String result = mentionUtils.renderHtml("<script>alert('xss')</script> @[Alice](userId:1)");

        assertThat(result).doesNotContain("<script>");
        assertThat(result).contains("&lt;script&gt;");
    }

    @Test
    void renderHtml_escapesHtmlInDisplayName() {
        String result = mentionUtils.renderHtml("@[<b>Evil</b>](userId:1)");

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
