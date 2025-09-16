import alt from "../../alt";
import ExportSearchResultsActions from "../../actions/workbench/ExportSearchResultsActions";

class ExportSearchResultsStore {

    constructor() {
        this.show = false;
        this.bindActions(ExportSearchResultsActions);
    }

    open() {
        this.show = true;
    }

    close() {
        this.show = false;
    }
}

export default alt.createStore(ExportSearchResultsStore, 'ExportSearchResultsStore');
