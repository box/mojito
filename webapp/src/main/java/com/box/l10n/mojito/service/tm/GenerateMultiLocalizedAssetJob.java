package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.rest.asset.AssetWithIdNotFoundException;
import com.box.l10n.mojito.rest.asset.LocaleInfo;
import com.box.l10n.mojito.rest.asset.LocalizedAssetBody;
import com.box.l10n.mojito.rest.asset.MultiLocalizedAssetBody;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Autowired;

public class GenerateMultiLocalizedAssetJob
    extends QuartzPollableJob<MultiLocalizedAssetBody, MultiLocalizedAssetBody> {

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired AssetRepository assetRepository;

  @Autowired RepositoryLocaleRepository repositoryLocaleRepository;

  @Autowired MeterRegistry meterRegistry;

  @Override
  public MultiLocalizedAssetBody call(MultiLocalizedAssetBody multiLocalizedAssetBody)
      throws Exception {

    Asset asset = assetRepository.findById(multiLocalizedAssetBody.getAssetId()).orElse(null);

    if (asset == null) {
      throw new AssetWithIdNotFoundException(multiLocalizedAssetBody.getAssetId());
    }

    return meterRegistry
        .timer(
            "GenerateMultiLocalizedAssetJob.call",
            Tags.of("repositoryName", asset.getRepository().getName()))
        .record(
            () -> {
              for (LocaleInfo localeInfo : multiLocalizedAssetBody.getLocaleInfos()) {

                RepositoryLocale repositoryLocale =
                    repositoryLocaleRepository.findByRepositoryIdAndLocaleId(
                        asset.getRepository().getId(), localeInfo.getLocaleId());

                String outputTag =
                    localeInfo.getOutputBcp47tag() != null
                        ? localeInfo.getOutputBcp47tag()
                        : repositoryLocale.getLocale().getBcp47Tag();
                QuartzJobInfo<LocalizedAssetBody, LocalizedAssetBody> quartzJobInfo =
                    QuartzJobInfo.newBuilder(GenerateLocalizedAssetJob.class)
                        .withInlineInput(false)
                        .withParentId(getParentId())
                        .withInput(createLocalizedAssetBody(localeInfo, multiLocalizedAssetBody))
                        .withScheduler(multiLocalizedAssetBody.getSchedulerName())
                        .withMessage(
                            "Generate localized asset for locale: "
                                + outputTag
                                + ", asset: "
                                + asset.getPath())
                        .build();
                multiLocalizedAssetBody.addGenerateLocalizedAddedJobIdToMap(
                    outputTag,
                    quartzPollableTaskScheduler
                        .scheduleJob(quartzJobInfo)
                        .getPollableTask()
                        .getId());
              }

              return multiLocalizedAssetBody;
            });
  }

  protected long getParentId() {
    return getCurrentPollableTask().getId();
  }

  private LocalizedAssetBody createLocalizedAssetBody(
      LocaleInfo localeInfo, MultiLocalizedAssetBody multiLocalizedAssetBody) {
    LocalizedAssetBody localizedAssetBody = new LocalizedAssetBody();
    localizedAssetBody.setLocaleId(localeInfo.getLocaleId());
    localizedAssetBody.setContent(multiLocalizedAssetBody.getSourceContent());
    localizedAssetBody.setAssetId(multiLocalizedAssetBody.getAssetId());
    localizedAssetBody.setOutputBcp47tag(localeInfo.getOutputBcp47tag());
    localizedAssetBody.setContent(multiLocalizedAssetBody.getSourceContent());
    localizedAssetBody.setFilterConfigIdOverride(
        multiLocalizedAssetBody.getFilterConfigIdOverride());
    localizedAssetBody.setFilterOptions(multiLocalizedAssetBody.getFilterOptions());
    localizedAssetBody.setInheritanceMode(multiLocalizedAssetBody.getInheritanceMode());
    localizedAssetBody.setPullRunName(multiLocalizedAssetBody.getPullRunName());
    localizedAssetBody.setStatus(multiLocalizedAssetBody.getStatus());
    return localizedAssetBody;
  }
}
