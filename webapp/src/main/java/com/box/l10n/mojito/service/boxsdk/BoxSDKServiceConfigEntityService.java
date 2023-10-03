package com.box.l10n.mojito.service.boxsdk;

import com.box.l10n.mojito.boxsdk.BoxAPIConnectionProvider;
import com.box.l10n.mojito.boxsdk.BoxSDKServiceException;
import com.box.l10n.mojito.boxsdk.MojitoAppUserInfo;
import com.box.l10n.mojito.entity.BoxSDKServiceConfigEntity;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.service.pollableTask.InjectCurrentTask;
import com.box.l10n.mojito.service.pollableTask.ParentTask;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxSharedLink;
import com.box.sdk.BoxUser;
import com.box.sdk.sharedlink.BoxSharedLinkRequest;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.ExecutionException;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author wyau
 */
@Service
public class BoxSDKServiceConfigEntityService {

    /**
     * logger
     */
    static Logger logger = getLogger(BoxSDKServiceConfigEntityService.class);

    private static final String MOJITO_FOLDER_NAME = "Mojito";
    private static final String PROJECT_REQUESTS_FOLDER_NAME = "Project Requests";

    @Autowired
    BoxSDKServiceConfigEntityRepository boxSDKServiceConfigEntityRepository;

    @Autowired
    BoxAPIConnectionProvider boxAPIConnectionProvider;

    @Autowired
    BoxSDKAppUserService boxSDKAppUserService;

    /**
     * @return
     */
    public BoxSDKServiceConfigEntity getBoxSDKServiceConfigEntity() {
        return boxSDKServiceConfigEntityRepository.findFirstByOrderByIdAsc();
    }

    /**
     * Add a new config
     *
     * @param clientId The Box API Client ID
     * @param clientSecret The Box API Client Secret
     * @param publicKeyId The Box API Public Key Id
     * @param privateKey The Box API Private Key
     * @param privateKeyPassword The Box API Private Key Password
     * @param enterpriseId The Enterprise ID that has authorized the above Client ID
     * @param currentTask
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws BoxSDKServiceException
     */
    @Pollable(async = true, message = "Start Adding Box SDK Service Config", expectedSubTaskNumber = 1)
    public PollableFuture<BoxSDKServiceConfigEntity> addConfig(
            String clientId, String clientSecret, String publicKeyId,
            String privateKey, String privateKeyPassword, String enterpriseId,
            @InjectCurrentTask PollableTask currentTask)
            throws ExecutionException, InterruptedException, BoxSDKServiceException {

        BoxSDKServiceConfigEntity boxSDKServiceConfig = boxSDKServiceConfigEntityRepository.findFirstByOrderByIdAsc();

        if (boxSDKServiceConfig != null) {
            throw new BoxSDKServiceException("Config must be deleted first before adding a new one");
        }

        boxSDKServiceConfig = new BoxSDKServiceConfigEntity(clientId, clientSecret, publicKeyId, privateKey, privateKeyPassword, enterpriseId,
                null, null, null, false);

        logger.debug("Initial saving of the config so that it can be used immediately");
        boxSDKServiceConfigEntityRepository.save(boxSDKServiceConfig);

        BoxUser.Info appUser = boxSDKAppUserService.createAppUser(
                boxSDKServiceConfig.getClientId(),
                boxSDKServiceConfig.getClientSecret(),
                boxSDKServiceConfig.getPublicKeyId(),
                boxSDKServiceConfig.getPrivateKey(),
                boxSDKServiceConfig.getPrivateKeyPassword(),
                boxSDKServiceConfig.getEnterpriseId()
        );

        boxSDKServiceConfig.setAppUserId(appUser.getID());

        logger.debug("Saving of the config with updated app user id: {}", appUser.getID());
        boxSDKServiceConfigEntityRepository.save(boxSDKServiceConfig);

        MojitoAppUserInfo mojitoFolderStructure = createMojitoFolderStructure();
        boxSDKServiceConfig.setRootFolderId(mojitoFolderStructure.getRootFolderId());
        boxSDKServiceConfig.setDropsFolderId(mojitoFolderStructure.getDropsFolderId());
        boxSDKServiceConfig.setBootstrap(true);

        validateConfig(boxSDKServiceConfig, currentTask);

        logger.debug("Saving of the config with updated IDs");
        boxSDKServiceConfigEntityRepository.save(boxSDKServiceConfig);

        return new PollableFutureTaskResult<>(boxSDKServiceConfig);
    }

    /**
     * Add a new config
     *
     * @param clientId The Box API Client ID
     * @param clientSecret The Box API Client Secret
     * @param publicKeyId The Box API Public Key Id
     * @param privateKey The Box API Private Key
     * @param privateKeyPassword The Box API Private Key Password
     * @param enterpriseId The Enterprise ID that has authorized the above Client ID
     * @param appUserId The Box App User that belongs to the Enterprise ID above
     * @param rootFolderId The root folder that contains all of Mojito related content and of which the App User has access to
     * @param dropsFolderId The folder that contains drops that the App User listed above has access to
     * @param currentTask
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws BoxSDKServiceException
     */
    @Pollable(async = true, message = "Start Adding Box SDK Service Config with no bootstrap", expectedSubTaskNumber = 1)
    public PollableFuture<BoxSDKServiceConfigEntity> addConfigWithNoBootstrap(
            String clientId, String clientSecret, String publicKeyId,
            String privateKey, String privateKeyPassword, String enterpriseId,
            String appUserId, String rootFolderId, String dropsFolderId,
            @InjectCurrentTask PollableTask currentTask)
            throws ExecutionException, InterruptedException, BoxSDKServiceException {

        BoxSDKServiceConfigEntity boxSDKServiceConfig = boxSDKServiceConfigEntityRepository.findFirstByOrderByIdAsc();

        if (boxSDKServiceConfig != null) {
            throw new BoxSDKServiceException("Config must be deleted first before adding a new one");
        }

        boxSDKServiceConfig = new BoxSDKServiceConfigEntity(clientId, clientSecret, publicKeyId, privateKey, privateKeyPassword, enterpriseId,
                appUserId, rootFolderId, dropsFolderId, false);

        logger.debug("Saving of the config first so that it can be validated");
        boxSDKServiceConfigEntityRepository.save(boxSDKServiceConfig);

        validateConfig(boxSDKServiceConfig, currentTask);

        return new PollableFutureTaskResult<>(boxSDKServiceConfig);
    }

    /**
     * Delete the {@link BoxSDKServiceConfigEntity}
     *
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws BoxSDKServiceException
     */
    @Transactional
    public void deleteConfig()
            throws ExecutionException, InterruptedException, BoxSDKServiceException {

        Long deleted = boxSDKServiceConfigEntityRepository.deleteFirstByOrderByIdAsc();
        logger.debug("Deleted Box SDK Config: {}", deleted);
    }

    /**
     * When root folder is not provided, create the following structure inside user's root.
     * <UserRoot>
     * |-> Mojito
     * |-> Project Requests
     * <p>
     * Note: We're creating a Mojito root folder to store everythign related to Mojito
     * because an App User can be accessible by other API keys
     * <p>
     * Note: for now, we're only creating the Project Requests (Drops) folder.
     * Maybe later when we extend our usage of the platform, we'll
     * need another folder to store other things
     */
    @Pollable(message = "Start Creating Mojito Folder Structure")
    private MojitoAppUserInfo createMojitoFolderStructure() throws BoxSDKServiceException {
        logger.debug("Creating Mojito Folder Structure");
        try {
            MojitoAppUserInfo result = new MojitoAppUserInfo();

            BoxAPIConnection apiConnection = boxAPIConnectionProvider.getConnection();
            BoxFolder parentFolder = new BoxFolder(apiConnection, BoxFolder.getRootFolder(apiConnection).getID());
            BoxFolder.Info mojitoFolder = parentFolder.createFolder(MOJITO_FOLDER_NAME);
            logger.debug("Created Mojito Folder: " + mojitoFolder.getID());
            result.setRootFolderId(mojitoFolder.getID());

            BoxFolder.Info projectRequestFolder = mojitoFolder.getResource().createFolder(PROJECT_REQUESTS_FOLDER_NAME);
            logger.debug("Created Project Requests Folder: " + projectRequestFolder.getID());
            result.setDropsFolderId(projectRequestFolder.getID());

            return result;
        } catch (BoxAPIException e) {
            throw new BoxSDKServiceException("Can't creating Mojito Folder Structure.", e);
        }
    }

    /**
     * @param boxSDKServiceConfig
     */
    @Pollable(message = "Validate Config Values")
    private void validateConfig(BoxSDKServiceConfigEntity boxSDKServiceConfig, @ParentTask PollableTask parentTask) {
        logger.debug("Validating Box SDK Config");
        try {
            BoxAPIConnection apiConnection = boxAPIConnectionProvider.getConnection();
            BoxFolder mojitoFolder = new BoxFolder(apiConnection, boxSDKServiceConfig.getRootFolderId());
            BoxSharedLink sharedLink = mojitoFolder.createSharedLink(new BoxSharedLinkRequest().access(BoxSharedLink.Access.COLLABORATORS));

            boxSDKServiceConfig.setRootFolderUrl(sharedLink.getURL());
            boxSDKServiceConfig.setValidated(true);

            boxSDKServiceConfigEntityRepository.save(boxSDKServiceConfig);

            logger.debug("Shared link for root folder: {}", sharedLink);
        } catch (BoxSDKServiceException e) {
            boxSDKServiceConfig.setValidated(false);
            logger.error("Error validing config", e);
        }
    }
}
