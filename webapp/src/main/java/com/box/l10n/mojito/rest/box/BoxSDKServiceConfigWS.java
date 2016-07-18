package com.box.l10n.mojito.rest.box;

import com.box.l10n.mojito.entity.BoxSDKServiceConfigEntity;
import com.box.l10n.mojito.service.boxsdk.BoxSDKServiceConfigEntityService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author wyau
 */
@RestController
public class BoxSDKServiceConfigWS {
    /**
     * logger
     */
    static Logger logger = getLogger(BoxSDKServiceConfigWS.class);

    @Autowired
    BoxSDKServiceConfigEntityService boxSDKServiceConfigEntityService;

    @RequestMapping(value = "/api/boxSDKServiceConfigs", method = RequestMethod.GET)
    public List<BoxSDKServiceConfigEntity> getBoxSDKServiceConfig() {
        List<BoxSDKServiceConfigEntity> result = new ArrayList<>();
        BoxSDKServiceConfigEntity boxSDKServiceConfigEntity = boxSDKServiceConfigEntityService.getBoxSDKServiceConfigEntity();

        if (boxSDKServiceConfigEntity != null) {
            result.add(boxSDKServiceConfigEntity);
        }

        return result;
    }

    @RequestMapping(value = "/api/boxSDKServiceConfigs", method = RequestMethod.PATCH)
    public ResponseEntity setBoxSDKServiceConfig(
            @RequestBody BoxSDKServiceConfigEntity config
    ) {
        try {
            logger.debug("Delete if config already exist");
            boxSDKServiceConfigEntityService.deleteConfig();

            PollableFuture<BoxSDKServiceConfigEntity> boxSDKServiceConfigEntityPollableFuture;

            if (!config.getBootstrap()) {
                boxSDKServiceConfigEntityPollableFuture = boxSDKServiceConfigEntityService.addConfigWithNoBootstrap(
                        config.getClientId(),
                        config.getClientSecret(),
                        config.getPublicKeyId(),
                        config.getPrivateKey(),
                        config.getPrivateKeyPassword(),
                        config.getEnterpriseId(),
                        config.getAppUserId(), config.getRootFolderId(),
                        config.getDropsFolderId()
                );
            } else {
                boxSDKServiceConfigEntityPollableFuture = boxSDKServiceConfigEntityService.addConfig(
                        config.getClientId(),
                        config.getClientSecret(),
                        config.getPublicKeyId(),
                        config.getPrivateKey(),
                        config.getPrivateKeyPassword(),
                        config.getEnterpriseId()
                );
            }

            return new ResponseEntity(boxSDKServiceConfigEntityPollableFuture, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Unable to update BoxSDKServiceConfig", e);
            return new ResponseEntity(null, HttpStatus.CONFLICT);
        }
    }
}
