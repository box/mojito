import _ from "lodash";

class LinkHelper {

    renderUrl(urlTemplate, urlComponentTemplate, params) {
        const url = this.renderTemplate(urlTemplate, params);
        const urlComponent = this.renderTemplate(urlComponentTemplate, params);
        return url + encodeURIComponent(urlComponent);
    }

    renderTemplate(template, params) {
        return _.template(template)(params);
    }
}

export default new LinkHelper();
