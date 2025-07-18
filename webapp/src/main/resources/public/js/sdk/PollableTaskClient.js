import BaseClient from "./BaseClient.js";
import PollableTask from "./entity/PollableTask.js";

/**
 * @param ms The amount of time in ms to delay
 * @return {Promise}
 */
function delay(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

class PollableTaskClient extends BaseClient {

    /**
     *
     * @param {Number} pollableId
     * @param {Number} timeout in ms
     * @return {Promise}
     */
    waitForPollableTaskToFinish(pollableId, timeout) {
        let timeoutTime = 0;
        if (timeout) {
            timeoutTime = (new Date()).getTime() + timeout;
        }

        return this.doWaitForPollableTaskToFinish(pollableId, timeoutTime);
    }

    /**
     * @param pollableId
     * @param timeoutTime The time at which waiting should stop.  If this is 0, it will wait forever.
     * @return {Promise.<TResult>}
     */
    doWaitForPollableTaskToFinish(pollableId, timeoutTime) {
        const currentTime = (new Date()).getTime();
        if (timeoutTime && currentTime > timeoutTime ) {
            throw new Error("Timed out waiting for pollableTask to finish");
        }

        return this.get(this.getUrl(pollableId), {}).then((json) => {
            const pollableTask = PollableTask.toPollableTask(json);

            if (!pollableTask.isAllFinished) {
                return delay(500).then(this.doWaitForPollableTaskToFinish.bind(this, pollableId, timeoutTime));
            }

            return pollableTask;
        });
    }

    /**
     * @return {string}
     */
    getEntityName() {
        return 'pollableTasks';
    }
}

export default new PollableTaskClient();



