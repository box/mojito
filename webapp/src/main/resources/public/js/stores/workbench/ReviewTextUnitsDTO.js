/**
 * Used to communicate with ReviewTextUnitDTOs WS
 */
export default class ReviewTextUnitsDTO {
    /**
     * @param {TextUnit[]} textUnits The textunit to perform the review action on.
     * @param {String} comment
     * @param {String} textUnitAction The action to perform on the text unit
     */
    constructor(textUnits, comment, textUnitAction) {

        /** @type {TextUnit[]} textUnits The textunit to perform the review action on. */
        this.textUnits = textUnits;
        /** @type {String} comment */
        this.comment = comment;
        /** @type {String} textUnitAction The action to perform on the text unit */
        this.textUnitAction = textUnitAction;
    }
}
