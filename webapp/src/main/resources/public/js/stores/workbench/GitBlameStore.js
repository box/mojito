import alt from "../../alt.js";
import TextUnitDataSource from "../../actions/workbench/TextUnitDataSource.js";
import GitBlameActions from "../../actions/workbench/GitBlameActions.js";
import GitBlameScreenshotViewerActions from "../../actions/workbench/GitBlameScreenshotViewerActions.js";
import GitBlameScreenshotViewerStore from "./GitBlameScreenshotViewerStore.js";

class GitBlameStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(GitBlameActions);
        this.bindActions(GitBlameScreenshotViewerActions);

        this.registerAsync(TextUnitDataSource);
    }

    setDefaultState() {
        this.show = false;
        this.textUnit = null;
        this.gitBlameWithUsage = null;
        this.loading = false;
    }

    close() {
        this.show = false;
    }

    openWithTextUnit(textUnit) {
        this.show = true;
        this.textUnit = textUnit;
        this.gitBlameWithUsage = null;
        this.loading = true;
        this.getInstance().getGitBlameInfo(textUnit);
    }

    onDeleteScreenshotSuccess() {
        this.gitBlameWithUsage.screenshots = GitBlameScreenshotViewerStore.state.branchStatisticScreenshots;
    }

    onGetGitBlameInfoSuccess(gitBlameWithUsage) {
        this.gitBlameWithUsage = gitBlameWithUsage[0];
        this.loading = false;
    }

    onGetGitBlameInfoError() {
        this.loading = false;
    }
}

export default alt.createStore(GitBlameStore, 'GitBlameStore');
