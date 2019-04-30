package com.box.l10n.mojito.smartling.response;

import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.request.BaseObject;
import com.box.l10n.mojito.smartling.request.Errors;
import com.ibm.icu.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseResponse {

    static Logger logger = LoggerFactory.getLogger(SmartlingClient.class);
    static final String API_SUCCESS_CODE = "SUCCESS";
    static final String API_INVALID_CREDENTIALS_CODE = "AUTHENTICATION_ERROR";

    public String formatErrors(Errors errors) {
        String msg = MessageFormat.format("{0}: {1}", errors.getKey(), errors.getMessage());
        if (errors.getDetails() != null) {
            msg = msg + MessageFormat.format("Further details: {0}", errors.getDetails());
        }
        logger.debug(msg);
        return msg;
    }

    public Boolean getIsSuccessResponse(BaseObject baseObject) {
        return API_SUCCESS_CODE.equals(baseObject.getCode());
    }

    public Boolean getIsAuthenticationErrorResponse(BaseObject baseObject) {
        return API_INVALID_CREDENTIALS_CODE.equals(baseObject.getCode());
    }

}
