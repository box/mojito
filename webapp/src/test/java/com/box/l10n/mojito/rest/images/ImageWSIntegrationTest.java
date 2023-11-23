package com.box.l10n.mojito.rest.images;

import static org.assertj.core.api.Assertions.assertThat;

import com.box.l10n.mojito.entity.Image;
import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.service.image.ImageService;
import java.util.Optional;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

public class ImageWSIntegrationTest extends WSTestBase {

  private static final String imageName = "1234-5678-9101-1121Some file name with gaps.png";

  private static final String PATH_PREFIX = "/api/images/";

  @Autowired ImageService imageService;

  @Test
  public void testUploadImage() {
    byte[] imageBytes = "test".getBytes();
    authenticatedRestTemplate.put(PATH_PREFIX + imageName, imageBytes);
    ResponseEntity<byte[]> response =
        authenticatedRestTemplate.getForEntity(PATH_PREFIX + imageName, byte[].class);
    assertThat(response.getStatusCode().is2xxSuccessful());
    assertThat(response.getBody()).isEqualTo(imageBytes);

    // Verify the stored image uses the HTTP decoded name
    Optional<Image> imageOpt = imageService.getImage(imageName);
    assertThat(imageOpt).isPresent();
    assertThat(imageOpt.get().getName()).isEqualTo(imageName);
  }
}
