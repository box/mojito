import Cldr from "cldrjs";
import _ from "lodash";

Cldr.load(require('cldr-data/supplemental/likelySubtags.json'));

//TODO Load all locale data for now in the bundle... as number of locale grows we should only used locale data.
Cldr.load(require('cldr-data/main/be/languages.json'));
Cldr.load(require('cldr-data/main/be/territories.json'));

Cldr.load(require('cldr-data/main/en/languages.json'));
Cldr.load(require('cldr-data/main/en/territories.json'));

Cldr.load(require('cldr-data/main/fr/languages.json'));
Cldr.load(require('cldr-data/main/fr/territories.json'));

Cldr.load(require('cldr-data/main/ko/languages.json'));
Cldr.load(require('cldr-data/main/ko/territories.json'));

Cldr.load(require('cldr-data/main/ru/languages.json'));
Cldr.load(require('cldr-data/main/ru/territories.json'));

Cldr.load(require('cldr-data/main/de/languages.json'));
Cldr.load(require('cldr-data/main/de/territories.json'));

Cldr.load(require('cldr-data/main/es/languages.json'));
Cldr.load(require('cldr-data/main/es/territories.json'));

Cldr.load(require('cldr-data/main/it/languages.json'));
Cldr.load(require('cldr-data/main/it/territories.json'));

Cldr.load(require('cldr-data/main/ja/languages.json'));
Cldr.load(require('cldr-data/main/ja/territories.json'));

Cldr.load(require('cldr-data/main/pt/languages.json'));
Cldr.load(require('cldr-data/main/pt/territories.json'));

Cldr.load(require('cldr-data/main/zh-Hans/languages.json'));
Cldr.load(require('cldr-data/main/zh-Hans/territories.json'));

Cldr.load(require('cldr-data/main/zh-Hant/languages.json'));
Cldr.load(require('cldr-data/main/zh-Hant/territories.json'));

class Locales {

    constructor() {
        //TODO don't use global var for LOCALE?
        this.currentLocale = LOCALE;

        this.cldr = new Cldr(this.currentLocale);
    }

    /**
     * Gets the list of supported locales in the app.
     *
     * Must add CLDR data accordingly, see above.
     *
     * @returns {string[]}
     */
    getSupportedLocales() {
        return [
            'be',
            'de',
            'en',
            'es',
            'fr',
            'it',
            'ja',
            'ko',
            'pt',
            'ru',
            'zh-Hans',
            'zh-Hant'
        ];
    }

    /**
     * Gets a list of language that are RTL
     * @returns {string[]}
     */
    getRightToLeftLanguages() {

        return [
            'ar',
            'arc',
            'bqi',
            'ckb',
            'dv',
            'fa',
            'glk',
            'he',
            'lrc',
            'mzn',
            'pnb',
            'ps',
            'sd',
            'ug',
            'ur',
            'yi'
        ];
    }


    /**
     * Gets the current locale of the app as a bcp47 tag
     *
     * @returns {string} bcp47 tag of the app locale
     */
    getCurrentLocale() {
        return this.currentLocale;
    }


    /**
     * Gets the display name of the current locale
     *
     * @returns {string} the display name of the current locale
     */
    getCurrentLocaleDisplayName() {
        return this.getDisplayName(this.getCurrentLocale());
    }

    /**
     * Gets the locale display name in current language, format: language (territory)
     *
     * @param bcp47Tag the bcp47 tag
     * @returns {string} the locale display name
     */
    getDisplayName(bcp47Tag) {

        const targetCldr = new Cldr(bcp47Tag);

        const language = targetCldr.attributes.language;
        const territory = targetCldr.attributes.territory;

        const languageDisplay = this.cldr.main("localeDisplayNames/languages/" + language);
        const regionDisplay = this.cldr.main("localeDisplayNames/territories/" + territory);

        return languageDisplay + ' (' + regionDisplay + ')';
    }

    /**
     * Gets the native display name of a locale given its bcp47 tag. Native means in the locale language of the given
     * locale, eg. getNativeDispalyName('be-BE') -> беларуская (Беларусь)
     *
     * @param bcp47Tag bcp47 tag of the locale
     * @returns {string} the native display name
     */
    getNativeDispalyName(bcp47Tag) {
        const targetCldr = new Cldr(bcp47Tag);

        const language = targetCldr.attributes.language;
        const territory = targetCldr.attributes.territory;

        const languageDisplay = targetCldr.main("localeDisplayNames/languages/" + language);
        const regionDisplay = targetCldr.main("localeDisplayNames/territories/" + territory);

        return languageDisplay + ' (' + regionDisplay + ')';
    }

    /**
     * Gets the direction (ltr or rtl) of a locale
     *
     * @param bcp47Tag bcp47 tag of the locale
     * @returns {string} the direction (ltr or rtl)
     */
    getLanguageDirection(bcp47Tag) {

        const targetCldr = new Cldr(bcp47Tag);
        const language = targetCldr.attributes.language;

        let dir = "ltr";

        if (_.includes(this.getRightToLeftLanguages(), language)) {
            dir = "rtl";
        }

        return dir;
    }

    /**
     * Returns sorted objects by the locale display name
     *
     * @param {Object[]} objects array of object that contains locale
     * @param {Function} getBcp45Tag function to retrieve bcp47Tag from the object
     * @returns {Object[]} sorted objects
     */
    sortByDisplayName(objects, getBcp47Tag) {
        return objects
            .map(object => {
                let bcp47Tag = getBcp47Tag(object);
                object.localeDisplayName = this.getDisplayName(bcp47Tag);
                return object;
            })
            .sort((a, b) => a.localeDisplayName.localeCompare(b.localeDisplayName));
    }

}
;

export default new Locales();



