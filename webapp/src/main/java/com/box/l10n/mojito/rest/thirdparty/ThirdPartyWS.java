package com.box.l10n.mojito.rest.thirdparty;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.service.thirdparty.ThirdPartyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ThirdPartyWS {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ThirdPartyWS.class);

    @Autowired
    ThirdPartyService thirdPartyService;

    @RequestMapping(value = "/api/thirdparty/sync", method = RequestMethod.POST)
    public PollableTask sync(@RequestBody ThirdPartySync thirdPartySync) {
        logger.debug("Sync repository: {}", thirdPartySync);
        return thirdPartyService.asyncSyncMojitoWithThirdPartyTMS(thirdPartySync)
                .getPollableTask();
    }
}
