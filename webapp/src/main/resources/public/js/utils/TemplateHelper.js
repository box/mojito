import _ from "lodash";

class TemplateHelper {

    renderTemplate(template, params) {
        return _.template(template)(params); //nosemgrep
    }
}

export default new TemplateHelper();
