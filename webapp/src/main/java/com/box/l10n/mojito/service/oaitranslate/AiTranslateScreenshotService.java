package com.box.l10n.mojito.service.oaitranslate;

import com.box.l10n.mojito.entity.Image;
import com.box.l10n.mojito.service.image.ImageService;
import com.box.l10n.mojito.util.ImageBytes;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AiTranslateScreenshotService {
  private static final Pattern UUID_PATTERN =
      Pattern.compile(
          "s:([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");

  ImageService imageService;

  public AiTranslateScreenshotService(ImageService imageService) {
    this.imageService = imageService;
  }

  public static String extractScreenshotUUID(String comment) {
    if (comment == null) {
      return null;
    }
    Matcher matcher = UUID_PATTERN.matcher(comment);
    return matcher.find() ? matcher.group(1) : null;
  }

  public Optional<ImageBytes> getImageBytes(String screenshotUUID) {
    Optional<Image> image = imageService.getImage(screenshotUUID);
    return image.map(i -> ImageBytes.fromBytes(i.getName(), i.getContent()));
  }
}
