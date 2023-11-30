package com.box.l10n.mojito.rest.clobstorage;

import com.box.l10n.mojito.rest.textunit.InvalidTextUnitSearchParameterException;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClobStorageWS {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ClobStorageWS.class);

  final StructuredBlobStorage structuredBlobStorage;

  public ClobStorageWS(StructuredBlobStorage structuredBlobStorage) {
    this.structuredBlobStorage = Objects.requireNonNull(structuredBlobStorage);
  }

  @RequestMapping(method = RequestMethod.POST, value = "/api/clobstorage")
  @ResponseStatus(HttpStatus.OK)
  public UUID post(@RequestBody String content) throws InvalidTextUnitSearchParameterException {

    final UUID uuid = UUID.randomUUID();

    structuredBlobStorage.put(
        StructuredBlobStorage.Prefix.CLOB_STORAGE_WS,
        uuid.toString(),
        content,
        Retention.PERMANENT);

    return uuid;
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/clobstorage/{uuid}")
  @ResponseStatus(HttpStatus.OK)
  public String getTextUnitsSeargetchParams(@PathVariable(value = "uuid") UUID uuid)
      throws InvalidTextUnitSearchParameterException {
    return structuredBlobStorage
        .getString(StructuredBlobStorage.Prefix.CLOB_STORAGE_WS, uuid.toString())
        .get();
  }
}
