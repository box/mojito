import alt from "../alt.js";

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
