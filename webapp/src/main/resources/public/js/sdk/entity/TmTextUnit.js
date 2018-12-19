export default class TmTextUnit {
    constructor() {
        /**
         *
         * @type {BigInt}
         */
        this.id = id;

        /**
         *
         * @type {String}
         */
        this.name = null;

        /**
         *
         * @type {String}
         */
        this.content = null;
    }

    static toTmTextUnit(json) {
        let result = new TmTextUnit();

        result.id = json.id;
        result.name = json.name;
        result.branchStatisticsContent = json.content;

        return result;
    }

}