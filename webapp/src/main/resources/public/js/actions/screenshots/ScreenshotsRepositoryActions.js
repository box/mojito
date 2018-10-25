import alt from "../../alt";

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

