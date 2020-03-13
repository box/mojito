package com.box.l10n.mojito.service.assetcontent;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.service.branch.BranchService;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service to manage {@link AssetContent}.
 * <p>
 * An {@link AssetContent} is linked to a branch. It is possible that the system is used without specifying branch name
 * (eg. in the CLI). If that's the case, the asset content is linked to  branch with name: null.
 * <p>
 * We don't use {@link AssetContentRepository} (not public) is not used directly to avoid badly handling the branch
 * with null name.
 *
 * @author jeanaurambault
 */
@Service
public class AssetContentService {

    /**
     * logger
     */
    static Logger logger = getLogger(AssetContentService.class);

    @Autowired
    BranchService branchService;

    @Autowired
    AssetContentRepository assetContentRepository;

    /**
     * Creates an {@link AssetContent} with no branch name specified. This will create a {@link Branch} with
     * branch name: null.
     *
     * @param asset
     * @param content
     * @return
     */
    public AssetContent createAssetContent(Asset asset, String content) {
        Branch branch = branchService.getUndeletedOrCreateBranch(asset.getRepository(), null, null);
        return createAssetContent(asset, content, false, branch);
    }

    /**
     * Creates an {@link AssetContent} for a given {@link Branch}.
     *
     * @param asset
     * @param content
     * @param extractedContent
     * @param branch
     * @return
     */
    public AssetContent createAssetContent(Asset asset, String content, boolean extractedContent, Branch branch) {
        logger.debug("Create asset content for asset id: {} and branch id: {}", asset.getId(), branch.getId());
        AssetContent assetContent = new AssetContent();

        assetContent.setAsset(asset);
        assetContent.setContent(content);
        assetContent.setContentMd5(DigestUtils.md5Hex(content));
        assetContent.setBranch(branch);
        assetContent.setExtractedContent(extractedContent);

        assetContent = assetContentRepository.save(assetContent);

        return assetContent;
    }

    /**
     * Proxy {@link AssetContentRepository#findOne()}.
     *
     * @param id {@link AssetContent#id}
     * @return the asset content
     */
    public AssetContent findOne(Long id) {
        return assetContentRepository.findOne(id);
    }
}
