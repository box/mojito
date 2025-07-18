import alt from "../../alt.js";

class GitBlameActions {

    constructor() {
        this.generateActions(
            "openWithTextUnit",
            "getGitBlameInfoSuccess",
            "getGitBlameInfoError",
            "close"
        );
    }
}

export default alt.createActions(GitBlameActions);
