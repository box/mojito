import $ from "jquery";
import alt from "../alt";

class RepositoryActions {

    init() {
        this.actions.getAllRepositories();
    }

    getAllRepositories() {

        //TODO this is not good it must use the SDK'

        $.get("/api/repositories").then(response => {
           this.dispatch(response);
        });
    }
}

export default alt.createActions(RepositoryActions);
