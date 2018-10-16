import _ from "lodash";

class LinkHelper {

    getLink(urlTemplate, params) {
        return _.template(urlTemplate)(params);
    }
}

export default new LinkHelper();
