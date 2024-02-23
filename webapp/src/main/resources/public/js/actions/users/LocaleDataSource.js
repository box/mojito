import LocaleClient from "../../sdk/LocaleClient";
import UserClient from "../../sdk/UserClient";
import LocaleActions from "./LocaleActions";

const LocaleDataSource = {
    loadLocales: {
        remote(userStoreState) {
            return LocaleClient.getAllLocales();
        },

        success: LocaleActions.loadLocalesSuccess,
        error: LocaleActions.loadLocalesError
    },
};

export default LocaleDataSource;
