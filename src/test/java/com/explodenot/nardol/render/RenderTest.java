package com.explodenot.nardol.render;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RenderTest {

    @Test
    void testTextWidthWithAnsiAndWideChars() {
        String plain = "Hello World";
        assertThat(TextWidth.displayWidth(plain)).isEqualTo(11);

        String ansi = "\u001B[31mHello\u001B[0m";
        assertThat(TextWidth.displayWidth(ansi)).isEqualTo(5);

        // Japanese kanji / Chinese characters are width 2
        String wide = "東京";
        assertThat(TextWidth.displayWidth(wide)).isEqualTo(4);

        String mixed = "Tokyo: 東京";
        assertThat(TextWidth.displayWidth(mixed)).isEqualTo(7 + 4);
    }

    @Test
    void testNoColorDepthProducesZeroAnsi() {
        Ansi ansi = new Ansi(ColorDepth.NONE);
        String styled = ansi.color(Rgb.RED, "Error text");
        assertThat(styled).isEqualTo("Error text");
        assertThat(styled).doesNotContain("\u001B[");
    }

    @Test
    void testAsciiGlyphsProduceZeroNonAscii() {
        Glyphs ascii = Glyphs.create(true);
        assertThat(ascii.ok()).matches("^[\\x00-\\x7F]+$");
        assertThat(ascii.degraded()).matches("^[\\x00-\\x7F]+$");
        assertThat(ascii.failed()).matches("^[\\x00-\\x7F]+$");
        assertThat(ascii.upArrow()).matches("^[\\x00-\\x7F]+$");
        assertThat(ascii.downArrow()).matches("^[\\x00-\\x7F]+$");
    }

    @Test
    void testSparkline() {
        List<Double> values = List.of(10.0, 20.0, 30.0, 40.0, 50.0);
        String spark = Sparkline.render(values, Glyphs.UNICODE);
        assertThat(spark).isEqualTo(" ▃▅▆█");

        String asciiSpark = Sparkline.render(values, Glyphs.ASCII);
        assertThat(asciiSpark).isEqualTo("_-~~=");
    }

    @Test
    void testHtmlEscaping() {
        String hostile = "<script>alert('xss')</script> & \"quotes\"";
        String escaped = HtmlReport.escapeHtml(hostile);
        assertThat(escaped).doesNotContain("<script>");
        assertThat(escaped).contains("&lt;script&gt;");
        assertThat(escaped).contains("&amp;");
        assertThat(escaped).contains("&quot;");
    }

    @Test
    void testWorldMapRenderWithPrintWriter() {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        WorldMap map = new WorldMap(new Ansi(ColorDepth.NONE), Glyphs.create(true), true);
        map.render(List.of(), null, null, 80, 20, pw);
        pw.flush();
        String output = sw.toString();
        assertThat(output).contains("╭");
        assertThat(output).contains("╰");
        assertThat(output).contains("Legend:");
    }
}
