package com.box.l10n.mojito.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author jaurambault
 */
public class StreamUtilTest {

    @Test
    public void testGetUTF8OutputStreamAsString() throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write("test".getBytes(StandardCharsets.UTF_8.name()));

        String utF8OutputStreamAsString = StreamUtil.getUTF8OutputStreamAsString(byteArrayOutputStream);
        assertEquals("test", utF8OutputStreamAsString);
    }

}
