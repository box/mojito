import LocaleClient from "../sdk/LocaleClient";
import LocaleActions from "./LocaleActions";

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
