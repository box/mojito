package com.box.l10n.mojito.service.assetcontent;

import com.box.l10n.mojito.entity.AssetContent;
import java.util.Optional;

public interface ContentService {
  Optional<String> getContent(AssetContent assetContent);

  void setContent(AssetContent assetContent, String content);
}
