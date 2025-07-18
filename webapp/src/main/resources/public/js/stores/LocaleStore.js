import alt from "../alt.js";
import LocaleActions from "../actions/LocaleActions.js";
import LocaleDataSource from "../actions/LocaleDataSource.js";

class LocaleStore {

    constructor() {
        this.locales = [];
        this.bindActions(LocaleActions);
        this.registerAsync(LocaleDataSource);
    }

    getLocales() {
        this.getInstance().getLocales();
    }

    getLocalesSuccess(locales) {
        this.locales = locales;
    }
}

export default alt.createStore(LocaleStore, 'LocaleStore');
