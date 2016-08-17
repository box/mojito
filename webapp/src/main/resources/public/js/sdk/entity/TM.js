import User from "./User";

export default class TM {
    constructor() {

        /** @type {User} */
        this.createdByUser = null;

        /** @type {Date} */
        this.createdDate = null;

        /** @type {Number} */
        this.id = 0;

        /** @type {Date} */
        this.lastModifiedDate = null;
    }

    /**
     * @param {Object} json
     * @return {TM}
     */
    static toTM(json) {
        let result = null;
        if (json) {
            result = new TM();
            result.createdByUser = User.toUser(json.createdByUser);
            result.createdDate = new Date(json.createdDate);
            result.id = json.id;
            result.lastModifiedDate = new Date(json.lastModifiedDate);
        }
        return result;
    }

    /**
     * @param {Object[]} jsons
     * @return {TM[]}
     */
    static toTMs(jsons) {
        let results = [];

        for (let json of jsons) {
            results.push(TM.toTM(json));
        }

        return results;
    }
}
