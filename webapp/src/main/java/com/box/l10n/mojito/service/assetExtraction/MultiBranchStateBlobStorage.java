package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.localtm.merger.MultiBranchState;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import com.google.common.base.Preconditions;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage.Prefix.MULTI_BRANCH_STATE;

@Component
public class MultiBranchStateBlobStorage {

    StructuredBlobStorage structuredBlobStorage;

    ObjectMapper objectMapper;

    public MultiBranchStateBlobStorage(StructuredBlobStorage structuredBlobStorage, ObjectMapper objectMapper) {
        this.structuredBlobStorage = Preconditions.checkNotNull(structuredBlobStorage);
        this.objectMapper = Preconditions.checkNotNull(objectMapper);
    }

    public Optional<MultiBranchState> getMultiBranchStateForAssetExtractionId(long assetExtractionId, long version) {
        Optional<String> string = structuredBlobStorage.getString(MULTI_BRANCH_STATE, getName(assetExtractionId,version));
        return string.map(s -> objectMapper.readValueUnchecked(s, MultiBranchState.class));
    }

    public void putMultiBranchStateForAssetExtractionId(MultiBranchState multiBranchState, long assetExtractionId, long version) {
        structuredBlobStorage.put(
                MULTI_BRANCH_STATE,
                getName(assetExtractionId, version),
                objectMapper.writeValueAsStringUnchecked(multiBranchState),
                Retention.PERMANENT);
    }

    public void deleteMultiBranchStateForAssetExtractionId(long assetExtractionId, long version) {
        structuredBlobStorage.delete(MULTI_BRANCH_STATE, getName(assetExtractionId, version));
    }

    String getName(long assetExtractionId, long version) {
        return "assetExtraction/" + assetExtractionId + "/version/" + version;
    }
}
