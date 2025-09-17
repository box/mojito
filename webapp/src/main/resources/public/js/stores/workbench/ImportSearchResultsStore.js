import alt from "../../alt";
import ImportSearchResultsActions from "../../actions/workbench/ImportSearchResultsActions";

class ImportSearchResultsStore {

    constructor() {
        this.show = false;
        this.bindActions(ImportSearchResultsActions);
    }

    open() {
        this.show = true;
    }

    close() {
        this.show = false;
    }
}

export default alt.createStore(ImportSearchResultsStore, 'ImportSearchResultsStore');

