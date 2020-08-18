package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Component
public class TranslatonBlobService {

    static Logger logger = LoggerFactory.getLogger(TranslatorWithInheritance.class);

    @Autowired
    StructuredBlobStorage structuredBlobStorage;

    @Autowired
    @Qualifier("fail_on_unknown_properties_false")
    ObjectMapper objectMapper;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    TextUnitUtils textUnitUtils;

    /**
     *
     */
    TextUnitDTO DELETED_TEXTUNIT_DTO = new TextUnitDTO();

    Map<String, TextUnitDTO> getTextUnitDTOsForLocaleByMD5New(Long assetId, Long localeId, StatusFilter statusFilter) {
        Optional<String> translationBlobString = structuredBlobStorage.getString(StructuredBlobStorage.Prefix.TRANSLATIONS, getBlobName(assetId, localeId));
        TranslationBlob translationBlob = translationBlobString.map(s -> objectMapper.readValueUnchecked(s, TranslationBlob.class)).orElse(new TranslationBlob());

        ImmutableMap<String, TextUnitDTO> fromBlob = translationBlob.getTextUnitDTOs().stream()
                .collect(ImmutableMap.toImmutableMap(getTextUnitDTOMd5(), Function.identity()));

        List<TextUnitDTO> deltaTextUnitDTOs = fetchDeltaForLocale(assetId, localeId, statusFilter);

        ImmutableMap<String, TextUnitDTO> fromDelta = deltaTextUnitDTOs.stream()
                .collect(ImmutableMap.toImmutableMap(getTextUnitDTOMd5(), Function.identity()));

        // order mater because we want to value from the delta first and drop fromBlob dupplicates
        ImmutableMap<String, TextUnitDTO> collect = Streams.concat(fromDelta.entrySet().stream(), fromBlob.entrySet().stream())
                .filter(e -> !DELETED_TEXTUNIT_DTO.equals(fromDelta.get(e.getKey()))) // TODO(perf) we don't have that - if test are not failing it means we're not testing that properly
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue, (useFromDelta, skipFromBlob) -> useFromDelta));


        TranslationBlob toWrite = new TranslationBlob();
        toWrite.setTextUnitDTOs(collect.values().asList());
        structuredBlobStorage.put(StructuredBlobStorage.Prefix.TRANSLATIONS, getBlobName(assetId, localeId), objectMapper.writeValueAsStringUnchecked(toWrite), Retention.PERMANENT);

//        return collect;
        return  fromDelta; // since we don't delete yet
    }


    private String getBlobName(Long assetId, Long localeId) {
        return "asset/" + assetId + "/locale/" + localeId;
    }

    Function<TextUnitDTO, String> getTextUnitDTOMd5() {
        return textUnitDTO -> textUnitUtils.computeTextUnitMD5(textUnitDTO.getName(), textUnitDTO.getSource(), textUnitDTO.getComment());
    }

    List<TextUnitDTO> fetchDeltaForLocale(Long assetId, Long localeId, StatusFilter statusFilter) {

        // TODO(perf) this is pretty much useless that this point since it fetches everything still.
        //
        // we remove reccord for deletes so we can only fetch the last current values. so we have to fetch everything
        // which is not great. also we can't filter by asset Id meaning we'd have to join
        //
        // 1. change
        //   keep record for delete (opiton to purge?)
        //   denormalize asset id

        // version that doesn't work because delete won't be fetched
        logger.debug("Prepare TextUnitSearcherParameters to fetch translation for locale: {}", localeId);
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setLocaleId(localeId);
        textUnitSearcherParameters.setAssetId(assetId);
        textUnitSearcherParameters.setStatusFilter(statusFilter);


        logger.debug("Getting TextUnitDTOs");
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

        return textUnitDTOs;
    }
}
