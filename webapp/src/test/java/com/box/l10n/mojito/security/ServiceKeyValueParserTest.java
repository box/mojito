package com.box.l10n.mojito.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class ServiceKeyValueParserTest {

  private ServiceKeyValueParser parser;

  @BeforeEach
  public void setUp() {
    parser = new ServiceKeyValueParser();
  }

  @Test
  public void testParseHeader_EmptyHeaderOrNull() {
    Executable executable = () -> parser.parseHeader("", "/", ",", "=", "id");
    assertThrows(ServiceNotIdentifiableException.class, executable);
    executable = () -> parser.parseHeader(null, "&", ",", "=", "id");
    assertThrows(ServiceNotIdentifiableException.class, executable);
  }

  @Test
  public void testParseHeader_ValidCsvInput() {
    String header = "key1=value1,key2=value2,id=someService";
    String result = parser.parseHeader(header, "&", ",", "=", "id");
    assertEquals("someService", result);
  }

  @Test
  public void testParseHeader_ValidMultipleServiceInstanceInput() {
    String header = "key1=value1,key2=value2,id=firstService&id=lastService";
    String result = parser.parseHeader(header, "&", ",", "=", "id");
    assertEquals("lastService", result);
  }

  @Test
  public void testParseHeader_ValidSemicolonInput() {
    String header = "key1:=value1;key2:=value2;id:=someService";
    String result = parser.parseHeader(header, "&", ";", ":=", "id");
    assertEquals("someService", result);
  }

  @Test
  public void testParseHeader_NoIdentifierKey() {
    String header = "key1=value1,key2=value2";
    Executable executable = () -> parser.parseHeader(header, "&", ",", "=", "id");
    ServiceNotIdentifiableException exception =
        assertThrows(ServiceNotIdentifiableException.class, executable);
    assertEquals("Service identifier not found", exception.getMessage());
  }

  @Test
  public void testParseHeader_InvalidFormat() {
    String header = "key1=value1&key2value2&id=someService";
    Executable executable = () -> parser.parseHeader(header, "-", ";", "=", "id");
    assertThrows(ServiceNotIdentifiableException.class, executable);
  }

  @Test
  public void testParseHeader_EmptyServiceIdentifier() {
    String header = "key1=value1,key2=value2,id=";
    Executable executable = () -> parser.parseHeader(header, "&", ",", "=", "id");
    ServiceNotIdentifiableException exception =
        assertThrows(ServiceNotIdentifiableException.class, executable);
    assertEquals("Service identifier not found", exception.getMessage());
  }
}
