package com.box.l10n.mojito.okapi.filters;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class AndroidXMLEncoderTest {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AndroidXMLEncoderTest.class);

    AndroidXMLEncoder androidXMLEncoder;

    @Before
    public void before() {
        androidXMLEncoder = new AndroidXMLEncoder(false);
        androidXMLEncoder.unescapeUtils = new UnescapeUtils();
    }

    @Test
    public void testEscapeCommonAnnotation() {
        String actual = androidXMLEncoder.escapeCommon("&lt;annotation font=\\\"title_emphasis\\\"&gt;Android&lt;/annotation&gt;!");
        assertEquals("<annotation font=\"title_emphasis\">Android</annotation>!", actual);
    }

    @Test
    public void testEscapeCommonB() {
        String actual = androidXMLEncoder.escapeCommon("&lt;b&gt;Android&lt;/b&gt;!");
        assertEquals("<b>Android</b>!", actual);
    }

    @Test
    public void testEscapeDoubleQuotes() {
        assertEquals("a\\\"b" , androidXMLEncoder.escapeDoubleQuotes("a\"b"));
    }
}


