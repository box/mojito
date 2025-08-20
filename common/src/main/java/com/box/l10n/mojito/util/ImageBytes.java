package com.box.l10n.mojito.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

public record ImageBytes(String filename, byte[] content, String contentType) {

  public ImageBytes {
    Objects.requireNonNull(filename, "filename must not be null");
    Objects.requireNonNull(content, "content must not be null");
    Objects.requireNonNull(contentType, "contentType must not be null");
    if (filename.isBlank()) throw new IllegalArgumentException("filename must not be blank");
    if (content.length == 0) throw new IllegalArgumentException("content must not be empty");
    if (contentType.isBlank()) throw new IllegalArgumentException("contentType must not be blank");
  }

  public static ImageBytes fromFile(Path path) throws IOException {
    byte[] bytes = Files.readAllBytes(path);
    String probed = Files.probeContentType(path);
    String contentType = probed != null ? probed : guessContentType(path, bytes);
    return new ImageBytes(path.getFileName().toString(), bytes, contentType);
  }

  public static ImageBytes fromFile(String filename) throws IOException {
    return fromFile(Path.of(filename));
  }

  public static ImageBytes fromBytes(String filename, byte[] content, String contentType) {
    return new ImageBytes(filename, content, contentType);
  }

  public static ImageBytes fromBytes(String filename, byte[] content) {
    return fromBytes(filename, content, guessContentType(Path.of(filename), content));
  }

  public String toDataUrl() {
    String base64 = Base64.getEncoder().encodeToString(content);
    return "data:" + contentType + ";base64," + base64;
  }

  private static String guessContentType(Path path, byte[] bytes) {
    String filename = path.getFileName().toString();
    String ext = "";
    int dot = filename.lastIndexOf('.');
    if (dot >= 0 && dot < filename.length() - 1) {
      ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
    switch (ext) {
      case "png":
        return "image/png";
      case "jpg":
      case "jpeg":
        return "image/jpeg";
      case "gif":
        return "image/gif";
      case "webp":
        return "image/webp";
      case "bmp":
        return "image/bmp";
      case "svg":
        return "image/svg+xml";
      case "pdf":
        return "application/pdf";
      default:
        if (isPng(bytes)) {
          return "image/png";
        }
        ;
        if (isJpeg(bytes)) {
          return "image/jpeg";
        }
        if (isGif(bytes)) {
          return "image/gif";
        }
        return "application/octet-stream";
    }
  }

  private static boolean isPng(byte[] b) {
    return b.length >= 8
        && (b[0] & 0xFF) == 0x89
        && b[1] == 0x50 /* P */
        && b[2] == 0x4E /* N */
        && b[3] == 0x47 /* G */
        && b[4] == 0x0D
        && b[5] == 0x0A
        && b[6] == 0x1A
        && b[7] == 0x0A;
  }

  private static boolean isJpeg(byte[] b) {
    return b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8;
  }

  private static boolean isGif(byte[] b) {
    return b.length >= 6
        && b[0] == 'G'
        && b[1] == 'I'
        && b[2] == 'F'
        && b[3] == '8'
        && (b[4] == '7' || b[4] == '9')
        && b[5] == 'a';
  }
}
