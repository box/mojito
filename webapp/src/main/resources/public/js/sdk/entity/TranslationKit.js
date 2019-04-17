import User from "./User";
import Locale from "./Locale";
import StatusFilter from "./StatusFilter";

export default class TranslationKit {
    constructor() {

        /** @type {User} */
        this.createdByUser =  null;

        /** @type {Date} */
        this.createdDate = null;

        /** @type {Number} */
        this.id =  1;

        /** @type {Date} */
        this.lastModifiedDate =  null;

        /** @type {Locale} */
        this.locale =  null;

        /** @type {String[]} */
        this.notFoundTextUnitIds = [];

        /** @type {Number} */
        this.numBadLanguageDetections =  0;

        /** @type {Number} */
        this.numSourceEqualsTarget =  0;

        /** @type {Number} */
        this.numTranslatedTranslationKitUnits =  0;

        /** @type {Number} */
        this.numTranslationKitUnits =  0;

        /** @type {StatusFilter}  */
        this.type = null;

        /** @type {Number} */
        this.wordCount =  0;

        /** @type {Boolean} */
        this.imported = false;
    }

    /**
     * @param {Object} json
     * @return {TranslationKit}
     */
    static toTranslationKit(json) {
        let result = new TranslationKit();

        result.createdByUser = User.toUser(json.createdByUser);
        result.createdDate = new Date(json.createdDate);
        result.id = json.id;
        result.lastModifiedDate = new Date(json.lastModifiedDate);
        result.locale = Locale.toLocale(json.locale);
        result.notFoundTextUnitIds = json.notFoundTextUnitIds;
        result.numBadLanguageDetections = json.numBadLanguageDetections;
        result.numSourceEqualsTarget = json.numSourceEqualsTarget;
        result.numTranslatedTranslationKitUnits = json.numTranslatedTranslationKitUnits;
        result.numTranslationKitUnits = json.numTranslationKitUnits;
        result.type = new StatusFilter(json.type);
        result.wordCount = json.wordCount;
        result.imported = json.imported;

        return result;
    }

    /**
     * @param {Object[]} jsons
     * @return {TranslationKit[]}
     */
    static toTranslationKits(jsons) {
        let results = [];

        if (jsons) {
            for (let json of jsons) {
                results.push(TranslationKit.toTranslationKit(json));
            }
        }

        return results;
    }
}
