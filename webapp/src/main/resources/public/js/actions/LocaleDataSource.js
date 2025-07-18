import LocaleClient from "../sdk/LocaleClient.js";
import LocaleActions from "./LocaleActions.js";

const LocaleDataSource = {
    getLocales: {
        remote() {
            return LocaleClient.getLocales();
        },

        success: LocaleActions.getLocalesSuccess,
        error: LocaleActions.getLocalesError
    }
};

export default LocaleDataSource;
