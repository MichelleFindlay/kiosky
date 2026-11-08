package kiosky;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small Markdown-to-HTML converter for the bundled help page. Supports headings,
 * paragraphs, bullet and numbered lists, block quotes, fenced code, tables,
 * horizontal rules, images, links, bold, italics and inline code.
 */
final class Markdown {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*?)\\s*#*\\s*$");
    private static final Pattern BULLET = Pattern.compile("^[-*+]\\s+(.*)$");
    private static final Pattern NUMBERED = Pattern.compile("^\\d+[.)]\\s+(.*)$");
    private static final Pattern RULE = Pattern.compile("^\\s*(-{3,}|\\*{3,}|_{3,})\\s*$");
    private static final Pattern TABLE_SEP = Pattern.compile("^\\s*\\|?\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)*\\|?\\s*$");
    private static final Pattern IMAGE = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)\\s]+)\\)");
    private static final Pattern LINK =Pattern.compile("\\[([^\\]]+)\\]\\(([^)\\s]+)\\)");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("\\*(?!\\s)(.+?)(?<!\\s)\\*");

    private Markdown() {}

    static String toHtml(String md) {
        String[] lines = md.replace("\r\n", "\n").split("\n", -1);
        StringBuilder out = new StringBuilder();
        StringBuilder para = new StringBuilder();
        String list = null;      // "ul" or "ol" while a list is open
        boolean quote = false;   // inside a block quote
        boolean code = false;    // inside a fenced code block

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String t = line.trim();

            if (t.startsWith("```")) {
                if (!code) {
                    flush(out, para);
                    list = closeList(out, list);
                    quote = closeQuote(out, quote);
                }
                out.append(code ? "</code></pre>\n" : "<pre><code>");
                code = !code;
                continue;
            }
            if (code) {
                out.append(esc(line)).append('\n');
                continue;
            }

            boolean isQuote = t.startsWith(">");
            if (quote && !isQuote) {
                flush(out, para);
                quote = closeQuote(out, true);
            }

            if (t.length() == 0) {
                flush(out, para);
                list = closeList(out, list);
                continue;
            }

            Matcher m;
            if ((m = HEADING.matcher(t)).matches()) {
                flush(out, para);
                list = closeList(out, list);
                int n = m.group(1).length();
                out.append("<h").append(n).append('>').append(inline(m.group(2)))
                   .append("</h").append(n).append(">\n");
                continue;
            }
            if (RULE.matcher(t).matches()) {
                flush(out, para);
                list = closeList(out, list);
                out.append("<hr>\n");
                continue;
            }
            if (isQuote) {
                list = closeList(out, list);
                if (!quote) {
                    flush(out, para);
                    out.append("<blockquote>\n");
                    quote = true;
                }
                appendText(para, t.substring(1).trim());
                continue;
            }
            if (t.startsWith("|") && i + 1 < lines.length && TABLE_SEP.matcher(lines[i + 1]).matches()) {
                flush(out, para);
                list = closeList(out, list);
                out.append("<div class=\"table\"><table>\n<thead>");
                row(out, t, "th");
                out.append("</thead>\n<tbody>\n");
                i++; // skip the separator row
                while (i + 1 < lines.length && lines[i + 1].trim().startsWith("|")) {
                    row(out, lines[++i].trim(), "td");
                }
                out.append("</tbody>\n</table></div>\n");
                continue;
            }

            String type = null;
            if ((m = BULLET.matcher(t)).matches()) type = "ul";
            else if ((m = NUMBERED.matcher(t)).matches()) type = "ol";
            if (type != null) {
                flush(out, para);
                if (!type.equals(list)) {
                    list = closeList(out, list);
                    out.append('<').append(type).append(">\n");
                    list = type;
                } else {
                    out.append("</li>\n");
                }
                out.append("<li>").append(inline(m.group(1)));
                continue;
            }

            // Indented text continues the current list item
            if (list != null && line.length() > t.length() && Character.isWhitespace(line.charAt(0))) {
                out.append(' ').append(inline(t));
                continue;
            }
            list = closeList(out, list);
            appendText(para, t);
        }

        if (code) out.append("</code></pre>\n");
        flush(out, para);
        closeList(out, list);
        closeQuote(out, quote);
        return out.toString();
    }

    private static void appendText(StringBuilder para, String t) {
        if (para.length() > 0) para.append(' ');
        para.append(t);
    }

    private static void flush(StringBuilder out, StringBuilder para) {
        if (para.length() == 0) return;
        out.append("<p>").append(inline(para.toString())).append("</p>\n");
        para.setLength(0);
    }

    private static String closeList(StringBuilder out, String list) {
        if (list != null) out.append("</li>\n</").append(list).append(">\n");
        return null;
    }

    private static boolean closeQuote(StringBuilder out, boolean quote) {
        if (quote) out.append("</blockquote>\n");
        return false;
    }

    private static void row(StringBuilder out, String line, String cell) {
        String s = line.trim();
        if (s.startsWith("|")) s = s.substring(1);
        if (s.endsWith("|")) s = s.substring(0, s.length() - 1);
        out.append("<tr>");
        for (String c : s.split("\\|", -1)) {
            out.append('<').append(cell).append('>').append(inline(c.trim()))
               .append("</").append(cell).append('>');
        }
        out.append("</tr>\n");
    }

    /** Inline formatting. Text between backticks is shown literally. */
    private static String inline(String s) {
        String[] parts = s.split("`", -1);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            boolean closed = i < parts.length - 1;
            if (i % 2 == 1 && closed) {
                b.append("<code>").append(esc(parts[i])).append("</code>");
            } else {
                if (i % 2 == 1) b.append('`'); // unmatched backtick
                String x = esc(parts[i]);
                x = IMAGE.matcher(x).replaceAll("<img src=\"$2\" alt=\"$1\">");
                x = LINK.matcher(x).replaceAll("<a href=\"$2\">$1</a>");
                x = BOLD.matcher(x).replaceAll("<strong>$1</strong>");
                x = ITALIC.matcher(x).replaceAll("<em>$1</em>");
                b.append(x);
            }
        }
        return b.toString();
    }

    private static String esc(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': b.append("&amp;"); break;
                case '<': b.append("&lt;"); break;
                case '>': b.append("&gt;"); break;
                case '"': b.append("&quot;"); break;
                default: b.append(c);
            }
        }
        return b.toString();
    }
}
