import alt from "../../alt";

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
