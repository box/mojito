package com.box.l10n.mojito.service.drop;

import com.box.l10n.mojito.boxsdk.BoxSDKServiceException;
import java.io.IOException;

public interface DropFile {
  String getName();

  String getContent() throws BoxSDKServiceException, IOException;
}
