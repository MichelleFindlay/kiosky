package kiosky;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MarkdownTest {

    private static String html(String md) {
        return Markdown.toHtml(md);
    }

    @Test
    public void headingsAndParagraphs() {
        assertEquals("<h1>Title</h1>\n<p>One line and the next.</p>\n", html("# Title\n\nOne line\nand the next."));
        assertEquals("<h3>Small</h3>\n", html("### Small ###"));
    }

    @Test
    public void inlineFormatting() {
        assertEquals("<p><strong>bold</strong> and <em>italic</em></p>\n", html("**bold** and *italic*"));
        assertEquals("<p><a href=\"https://x.example\">site</a></p>\n", html("[site](https://x.example)"));
        assertEquals("<p><img src=\"logo.png\" alt=\"Logo\"></p>\n", html("![Logo](logo.png)"));
    }

    @Test
    public void codeIsLiteral() {
        assertEquals("<p><code>**not bold** &lt;b&gt;</code></p>\n", html("`**not bold** <b>`"));
        assertEquals("<pre><code>a &lt; b\n**x**\n</code></pre>\n", html("```\na < b\n**x**\n```")); // escaped, not formatted
    }

    @Test
    public void unclosedCodeBlockStillCloses() {
        assertEquals("<pre><code>dangling\n</code></pre>\n", html("```\ndangling"));
    }

    @Test
    public void escapesHtml() {
        String out = html("<script>alert(\"x\")</script> & more");
        assertFalse(out.contains("<script>"));
        assertTrue(out.contains("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt; &amp; more"));
    }

    @Test
    public void lists() {
        assertEquals("<ul>\n<li>one</li>\n<li>two continued</li>\n</ul>\n", html("- one\n- two\n  continued"));
        assertEquals("<ol>\n<li>first</li>\n<li>second</li>\n</ol>\n", html("1. first\n2. second"));
    }

    @Test
    public void tables() {
        String out = html("| A | B |\n| --- | --- |\n| `x` | **z** |");
        assertTrue(out.startsWith("<div class=\"table\"><table>\n<thead><tr><th>A</th><th>B</th></tr>\n"));
        assertTrue(out.contains("<tbody>\n<tr>"));
        assertTrue(out.contains("<td><strong>z</strong></td>"));
    }

    @Test
    public void quotesAndRules() {
        assertEquals("<blockquote>\n<p>Note this.</p>\n</blockquote>\n", html("> Note\n> this."));
        assertEquals("<p>a</p>\n<hr>\n<p>b</p>\n", html("a\n\n---\n\nb"));
    }

    @Test
    public void windowsLineEndings() {
        assertEquals(html("# T\n\ntext"), html("# T\r\n\r\ntext"));
    }
}
