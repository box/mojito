package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.ThirdPartyFileChecksum;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;

public class ThirdPartyTMSUtils {

  public static boolean isFileEqualToPreviousRun(
      ThirdPartyFileChecksumRepository thirdPartyFileChecksumRepository,
      Repository repository,
      Locale locale,
      String fileName,
      String fileContent,
      MeterRegistry meterRegistry) {

    boolean isChecksumEqual = false;

    String currentChecksum = DigestUtils.md5Hex(fileContent);

    Optional<ThirdPartyFileChecksum> thirdPartyFileChecksumOpt =
        thirdPartyFileChecksumRepository.findByRepositoryAndFileNameAndLocale(
            repository, fileName, locale);
    if (thirdPartyFileChecksumOpt.isPresent()
        && thirdPartyFileChecksumOpt.get().getMd5().equals(currentChecksum)) {
      isChecksumEqual = true;
    } else if (thirdPartyFileChecksumOpt.isPresent()) {
      ThirdPartyFileChecksum thirdPartyFileChecksum = thirdPartyFileChecksumOpt.get();
      thirdPartyFileChecksum.setMd5(currentChecksum);
      thirdPartyFileChecksum.setLastModifiedDate(JSR310Migration.newDateTimeEmptyCtor());
      thirdPartyFileChecksumRepository.save(thirdPartyFileChecksum);
    } else {
      thirdPartyFileChecksumRepository.save(
          new ThirdPartyFileChecksum(repository, fileName, locale, currentChecksum));
    }

    meterRegistry
        .counter(
            "ThirdPartyTMSUtils.deltaPullFilesProcessed",
            Tags.of(
                "repository",
                repository.getName(),
                "locale",
                locale.getBcp47Tag(),
                "fileName",
                fileName,
                "shortCircuited",
                Boolean.toString(isChecksumEqual)))
        .increment();

    return isChecksumEqual;
  }
}
