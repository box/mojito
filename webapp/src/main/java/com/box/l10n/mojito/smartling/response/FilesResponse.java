package com.box.l10n.mojito.smartling.response;

import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("response")
public class FilesResponse extends Response<Items<File>> {}
