package kiosky;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Translations of the help page must keep up with help.md: the same sections,
 * tables, table rows and code blocks, so a section added in English and forgotten in
 * a translation fails here.
 */
public class HelpTest {

    private static final File ASSETS = new File("app/assets");

    private static String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private static List<File> translations() {
        List<File> out = new ArrayList<File>();
        File[] all = ASSETS.listFiles();
        for (int i = 0; all != null && i < all.length; i++) {
            if (all[i].getName().matches("help\\.[a-z]{2,3}\\.md")) out.add(all[i]);
        }
        return out;
    }

    /** {h2, h3, tables, table rows, code fences, {{version}}} */
    private static int[] shape(String md) {
        int[] s = new int[6];
        for (String line : md.split("\n")) {
            if (line.startsWith("## ")) s[0]++;
            else if (line.startsWith("### ")) s[1]++;
            if (line.startsWith("| --- ")) s[2]++;
            if (line.startsWith("|")) s[3]++;
            if (line.startsWith("```")) s[4]++;
            if (line.contains("{{version}}")) s[5]++;
        }
        return s;
    }

    @Test
    public void thereAreTranslations() {
        assertFalse(translations().isEmpty());
    }

    @Test
    public void translationsMatchTheEnglishStructure() throws Exception {
        int[] english = shape(read(new File(ASSETS, "help.md")));
        String[] what = {"## sections", "### sections", "tables", "table rows", "code fences", "{{version}}"};
        for (File f : translations()) {
            int[] t = shape(read(f));
            for (int i = 0; i < english.length; i++) {
                assertEquals(f.getName() + ": " + what[i], english[i], t[i]);
            }
        }
    }

    @Test
    public void everyHelpPageRenders() throws Exception {
        List<File> all = translations();
        all.add(new File(ASSETS, "help.md"));
        for (File f : all) {
            String html = Markdown.toHtml(read(f));
            assertTrue(f.getName(), html.startsWith("<p><img src=\"logo.png\""));
            assertTrue(f.getName(), html.contains("<h1>"));
            assertEquals(f.getName() + " has an unclosed code block", 0, shape(read(f))[4] % 2);
        }
    }

    @Test
    public void languageCodes() {
        assertEquals("de", Pages.helpLanguage("de"));
        assertEquals("", Pages.helpLanguage("en"));
        assertEquals("he", Pages.helpLanguage("iw")); // Java's old code for Hebrew
        assertEquals("id", Pages.helpLanguage("in"));
        assertEquals("", Pages.helpLanguage(null));
        assertEquals("", Pages.helpLanguage("../x"));
    }
}
