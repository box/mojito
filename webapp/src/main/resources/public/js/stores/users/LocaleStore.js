import alt from "../../alt";
import Locale from "../../sdk/entity/Locale";
import LocaleActions from "../../actions/users/LocaleActions";
import LocaleDataSource from "../../actions/users/LocaleDataSource";

class LocaleStore {
    constructor() {
        /** @type {Locale[]} */
        this.allLocales = [];

        this.bindActions(LocaleActions);
        this.registerAsync(LocaleDataSource);
    }

    onLoadLocales(pageRequestParams) {
        // Only load once
        if (this.allLocales.length > 0) {
            return;
        }
        this.getInstance().loadLocales(pageRequestParams);
    }

    /**
     * @param {Locale[]} allLocales
     */
    onLoadLocalesSuccess(allLocales) {
        this.allLocales = allLocales;
    }

    onLoadLocalesError(err) {
        console.log("error fetching users", err);
    }
}

export default alt.createStore(LocaleStore, 'LocaleStore');
