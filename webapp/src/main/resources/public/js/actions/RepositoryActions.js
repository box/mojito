import alt from "../alt";

class RepositoryActions {
    constructor() {
        this.generateActions(
            "getAllRepositories",
            "getAllRepositoriesSuccess",
            "getAllRepositoriesError",
            "createRepository",
            "createRepositorySuccess",
            "createRepositoryError"
        );
    }
}

export default alt.createActions(RepositoryActions);
