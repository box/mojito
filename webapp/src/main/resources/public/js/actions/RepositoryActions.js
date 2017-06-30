import alt from "../alt";

class RepositoryActions {
    constructor() {
        this.generateActions(
            "getAllRepositories",
            "getAllRepositoriesSuccess",
            "getAllRepositoriesError"
        );
    }
}

export default alt.createActions(RepositoryActions);
