package com.box.l10n.mojito.rest.images;

import static org.assertj.core.api.Assertions.assertThat;

import com.box.l10n.mojito.apiclient.ImageClient;
import com.box.l10n.mojito.entity.Image;
import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.service.image.ImageService;
import com.box.l10n.mojito.test.category.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

public class ImageWSITest extends WSTestBase {

  @Autowired ImageClient imageClient;

  @Autowired ImageService imageService;

  @Test
  @Category({IntegrationTest.class})
  public void testImageNameOneWord() {
    final String imageName = "oneword.jpg";
    imageClient.uploadImage(imageName, "some test".getBytes(StandardCharsets.UTF_8));
    final Optional<Image> image = imageService.getImage(imageName);
    assertThat(image.get().getName()).isEqualTo(imageName);
  }

  @Test
  @Category({IntegrationTest.class})
  public void testImageNameWithSpace() {
    final String imageName = "some image.jpg";
    imageClient.uploadImage(imageName, "some test".getBytes(StandardCharsets.UTF_8));

    final Optional<Image> withWrongName = imageService.getImage("some%20image.jpg");
    assertThat(withWrongName.isPresent()).isFalse();

    final Optional<Image> image = imageService.getImage(imageName);
    assertThat(image.get().getName()).isEqualTo(imageName);
  }

  @Test
  @Category({IntegrationTest.class})
  public void testImageNameWithUTF8() {
    final String imageName = "こんにちは.jpg";
    imageClient.uploadImage(imageName, "some test".getBytes(StandardCharsets.UTF_8));

    final Optional<Image> withWrongName =
        imageService.getImage("%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF.jpg");
    assertThat(withWrongName.isPresent()).isFalse();

    final Optional<Image> image = imageService.getImage(imageName);
    assertThat(image.get().getName()).isEqualTo(imageName);
  }
}
