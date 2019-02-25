package com.box.l10n.mojito.okapi.filters;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnescapeFilterTest {

    UnescapeFilter unescapeFilter = new UnescapeFilter();

    @Test
    public void replaceCarriageReturn() {
        assertEquals("\r", unescapeFilter.replaceEscapedCarriageReturn("\\r"));
    }

    @Test
    public void replaceLineFeed() {
        assertEquals("\n", unescapeFilter.replaceEscapedLineFeed("\\n"));
    }

    @Test
    public void replaceEscapedCharacters() {
        assertEquals(".", unescapeFilter.replaceEscapedCharacters("\\."));
        assertEquals("@", unescapeFilter.replaceEscapedCharacters("\\@"));
        assertEquals("?", unescapeFilter.replaceEscapedCharacters("\\?"));
    }

    @Test
    public void replaceEscapedQuotes() {
        assertEquals("\" '", unescapeFilter.replaceEscapedQuotes("\\\" \\'"));
    }

    @Test
    public void collapseSpaces() {
        assertEquals(" a b c ", unescapeFilter.collapseSpaces("   a   b   c  "));
    }

    @Test
    public void replaceLineFeedWithSpace() {
        assertEquals("  a  ", unescapeFilter.replaceLineFeedWithSpace("\n a \n"));
    }

    @Test
    public void unescape() {
        assertEquals("  ' \" \n  ", unescapeFilter.unescape("  \' \\\" \\n  "));
    }
}