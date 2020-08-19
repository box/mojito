package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.ltm.merger.MultiBranchState;
import com.box.l10n.mojito.ltm.merger.MultiBranchStateJson;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MultiBranchStateJsonService {

    @Autowired
    StructuredBlobStorage structuredBlobStorage;

    @Autowired
    ObjectMapper objectMapper;

    public MultiBranchStateJson readMultiBranchStateJson(long assetId) {
        Optional<String> string = structuredBlobStorage.getString(StructuredBlobStorage.Prefix.MERGE_STATE, "shared_" + assetId);
        return (MultiBranchStateJson) string.map(s -> objectMapper.readValueUnchecked(s, MultiBranchStateJson.class)).orElse(new MultiBranchStateJson());
    }

    public MultiBranchStateJson readMultiBranchStateOfBranchJson(long assetExtractionByBranchId) {
        Optional<String> string = structuredBlobStorage.getString(StructuredBlobStorage.Prefix.MERGE_STATE, "branch_" + assetExtractionByBranchId);
        return (MultiBranchStateJson) string.map(s -> objectMapper.readValueUnchecked(s, MultiBranchStateJson.class)).orElse(new MultiBranchStateJson());
    }

}
