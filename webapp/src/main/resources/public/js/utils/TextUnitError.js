class TextUnitError extends Error {
    constructor(errorId, textUnit) {
        super();

        /** @type {TextUnit} */
        this.textUnit = textUnit;

        /** @type {Error.IDS} */
        this.errorId = errorId;
    }
}

export default TextUnitError;
