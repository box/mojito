import alt from "../alt";
import LocaleActions from "../actions/LocaleActions";
import LocaleDataSource from "../actions/LocaleDataSource";

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
