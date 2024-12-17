import alt from "../../alt";

import BoxSDKConfigActions from "../../actions/boxsdk/BoxSDKConfigActions";
import BoxSDKConfigDataSource from "./BoxSDKConfigDataSource";
import PollableTaskClient from "../../sdk/PollableTaskClient";

class BoxSDKConfigStore {

    constructor() {

        /** @type {BoxSDKConfig} */
        this.boxSDKConfig = null;

        /** @type {PollableTaskFuture} */
        this.boxSDKConfigPollableTaskFuture = null;

        /** @Type {Boolean} */
        this.isBeingProcessed = false;

        this.bindActions(BoxSDKConfigActions);

        this.registerAsync(BoxSDKConfigDataSource);
    }

    /**
     */
    onGetConfig() {
        this.getInstance().getConfig();
    }

    /**
     * @param {BoxSDKConfig} result
     */
    onGetConfigSuccess(result) {
        this.setState({ "boxSDKConfig": result });
    }

    onGetConfigError() {

    }

    /**
     * @param {BoxSDKConfig} boxSDKConfig
     */
    onSetConfig(boxSDKConfig) {
        this.getInstance().setConfig(boxSDKConfig);
    }

    /**
     * @param {PollableTaskFuture} pollableTaskFuture
     */
    onSetConfigSuccess(pollableTaskFuture) {
        this.boxSDKConfigPollableTaskFuture = pollableTaskFuture;

        const pollableTask = pollableTaskFuture.pollableTask;
        if (!pollableTask.isAllFinished) {
            this.setState({ "isBeingProcessed": true });
            PollableTaskClient.waitForPollableTaskToFinish(pollableTask.id)
                .then(() => {
                    this.onGetConfig();
                    this.setState({ "isBeingProcessed": false });
                });
        } else {
            this.isBeingProcessed = false;
        }
    }

    onSetConfigError() {

    }
}

export default alt.createStore(BoxSDKConfigStore, 'BoxSDKConfigStore');

