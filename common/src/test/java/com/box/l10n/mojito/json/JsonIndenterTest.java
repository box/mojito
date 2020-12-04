package com.box.l10n.mojito.json;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonIndenterTest {

    @Test
    public void ident() {
        assertEquals("{\n" +
                        "  \"a\" : {\n" +
                        "    \"b\" : [ \"1\", \"2\" ]\n" +
                        "  }\n" +
                        "}",
                JsonIndenter.indent("{\"a\":{\"b\":[\"1\",\"2\"]}}"));
    }

}
