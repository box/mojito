import alt from "../alt";
import RepositoryClient from "../sdk/RepositoryClient";

class RepositoryActions {

    init() {
        this.getAllRepositories();
    }

    getAllRepositories() {
        return (dispatch) => RepositoryClient.getRepositories().then(response => {
           dispatch(response);
        });
    }
}

export default alt.createActions(RepositoryActions);
