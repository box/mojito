import _ from "lodash";

class TemplateHelper {

    renderTemplate(template, params) {
        return _.template(template)(params);
    }
}

export default new TemplateHelper();
