import keymirror from "keymirror";

// TODO rename this so we dont' get confused by the default Error
class Error {
    constructor() {
        this.errorId = "";
    }

    /**
     * @returns {string} The errorId of this instance
     */
    getErrorId() {
        return this.errorId;
    }

    /**
     * Sets the errorId of this instance. Throws error if errorId is invalid.
     * @param {string} errorId A valid error id from the IDs map.
     */
    setErrorId(errorId) {
        if (!Error.IDS[errorId]) {
            throw "Invalid Error ID";
        }
        this.errorId = errorId;
    }
}

/**
 * TODO change this from keymirror to Symbol
 * Unique IDs to identify errors.
 */
Error.IDS = keymirror({
    "TEXTUNIT_SAVE_FAILED": null,
    "TEXTUNIT_CHECK_AND_SAVE_FAILED": null,
    "TEXTUNIT_CHECK_FAILED": null,
    "TEXTUNIT_DELETE_FAILED": null,
    "SEARCH_QUERY_FAILED": null,
    "VIRTUAL_ASSET_TEXTUNIT_SAVE_FAILED": null
});

/**
 * A map of error IDs to message keys in the properties file
 */
Error.MESSAGEKEYS_MAP = {
    "TEXTUNIT_SAVE_FAILED": "textUnit.save.failed",
    "TEXTUNIT_CHECK_AND_SAVE_FAILED": "textUnit.checkandsave.failed",
    "TEXTUNIT_CHECK_FAILED": "textUnit.check.failed",
    "TEXTUNIT_DELETE_FAILED": "textUnit.delete.failed",
    "VIRTUAL_ASSET_TEXTUNIT_SAVE_FAILED": "textUnit.saveVirtualAssetTextUnit.failed",
    "SEARCH_QUERY_FAILED": "search.query.failed"
};

export default Error;
