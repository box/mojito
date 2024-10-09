import Locale from "./Locale";

export default class UserLocale {
    constructor() {
        /** @type {Number} */
        this.id = 0;

        /** @type {Locale} */
        this.locale = null;
    }

    /**
     * Convert JSON UserLocale object
     *
     * @param {Object} jsonUserLocale
     * @return {UserLocale}
     */
    static toUserLocale(jsonUserLocale) {
        let userLocale = null;
        if (jsonUserLocale) {
            userLocale = new UserLocale();
            userLocale.id = jsonUserLocale.id;
            userLocale.locale = Locale.toLocale(jsonUserLocale.locale);
        }
        return userLocale;
    }
}
