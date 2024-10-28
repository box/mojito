import alt from "../../alt";

class AiReviewActions {

    constructor() {
        this.generateActions(
            "openWithTextUnit",
            "getAiReviewSuccess",
            "getAiReviewError",
            "close"
        );
    }
}

export default alt.createActions(AiReviewActions);
