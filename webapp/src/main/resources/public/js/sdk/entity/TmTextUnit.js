export default class TmTextUnit {
    constructor() {
        /**
         *
         * @type {BigInt}
         */
        this.id = null;

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

        /**
         *
         * @type {boolean}
         */
        this.screenshotUploaded = false;
    }

    static toTmTextUnit(json) {
        const result = new TmTextUnit();

        if (json) {
            result.id = json.id;
            result.name = json.name;
            result.content = json.content;
        }

        return result;
    }

}