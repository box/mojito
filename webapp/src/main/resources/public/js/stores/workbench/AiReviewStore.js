import alt from "../../alt";
import TextUnitDataSource from "../../actions/workbench/TextUnitDataSource";
import AIReviewActions from "../../actions/workbench/AiReviewActions";

class AIReviewStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(AIReviewActions);

        this.registerAsync(TextUnitDataSource);
    }

    setDefaultState() {
        this.show = false;
        this.textUnit = null;
        this.review = null;
        this.loading = false;
    }

    close() {
        this.show = false;
    }

    openWithTextUnit(textUnit) {
        this.show = true;
        this.textUnit = textUnit;
        this.review = null;
        this.loading = true;
        this.getInstance().getAiReview(textUnit);
    }

    onGetAiReviewSuccess(protoAiReviewResponse) {
        console.log("AIReviewStore::onGetAiReviewInfoSuccess");
        this.review = protoAiReviewResponse.aiReviewOutput;
        console.log("\n\n\n\n" + this.review + "\n\n\n\n")
        this.loading = false;
    }

    onGetAiReviewError(errorResponse) {
        console.log("AIReviewStore::onGetAiReviewInfoError");
        this.loading = false;
    }
}

export default alt.createStore(AIReviewStore, 'AIReviewStore');
