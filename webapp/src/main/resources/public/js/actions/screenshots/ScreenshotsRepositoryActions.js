import alt from "../../alt.js";

class ScreenshotsRepositoryActions {

    constructor() {
        this.generateActions(
            "getAllRepositories",
            "getAllRepositoriesSuccess",
            "getAllRepositoriesError",
            "changeSelectedRepositoryIds",
            "changeDropdownOpen"
        );
    }
}

export default alt.createActions(ScreenshotsRepositoryActions);

