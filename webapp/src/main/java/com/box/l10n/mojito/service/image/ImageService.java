package com.box.l10n.mojito.service.image;


import com.box.l10n.mojito.entity.Image;

import java.util.Optional;

public interface ImageService {

    Optional<Image> getImage(String name);

    void uploadImage(String name, byte[] content);

}
