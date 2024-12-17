import _ from "lodash";
import React from "react";
import TemplateHelper from "./TemplateHelper";


class LinkHelper {

    renderLinkOrLabel(urlTemplate, labelTemplate, params) {
        let linkOrLabel;

        const paramsWithEncoded = Object.assign({}, params, this.getEncodedParameters(params));

        let label = TemplateHelper.renderTemplate(labelTemplate, paramsWithEncoded);
        let url = TemplateHelper.renderTemplate(urlTemplate, paramsWithEncoded);

        if (url) {
            linkOrLabel = <a href={url}>{label}</a>;
        } else {
            linkOrLabel = label;
        }

        return linkOrLabel;
    }

    getEncodedParameters(params) {

        const encodedParams = Object.fromEntries(
            Object.entries(params).map(
                ([key, value]) => {
                    return ["encoded" + _.upperFirst(key), encodeURIComponent(value)];
                }
            )
        );

        return encodedParams;
    };
}

export default new LinkHelper();
