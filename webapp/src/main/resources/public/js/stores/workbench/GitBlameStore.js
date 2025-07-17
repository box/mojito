import alt from "../../alt";
import TextUnitDataSource from "../../actions/workbench/TextUnitDataSource";
import GitBlameActions from "../../actions/workbench/GitBlameActions";
import GitBlameScreenshotViewerActions from "../../actions/workbench/GitBlameScreenshotViewerActions";
import GitBlameScreenshotViewerStore from "./GitBlameScreenshotViewerStore";

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
