const TRANSLATION = "TRANSLATION";
const REVIEW = "REVIEW";

export default class StatusFilter {

    /**
     * @param {String} value Only values from StatusFilter.Type is supported
     */
    constructor(value) {
        let status;
        switch (value) {
            case TRANSLATION:
                status = TRANSLATION;
                break;
            case REVIEW:
                status = REVIEW;
                break;
            default:
                throw Error("Unknown value");
        }

        /** @type {String} */
        this.status = status;
    }

    /**
     * This is so that when StatusFilter is included in another entity and we when JSON.stringify, the we can have just the value
     * @return {String}
     */
    toJSON() {
        return this.status;
    }

    equals(statusFilter) {
        return this.status === statusFilter.status;
    }
}

StatusFilter.Type = {
    Translation: new StatusFilter("TRANSLATION"),
    Review: new StatusFilter("REVIEW")
};
