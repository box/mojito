import BaseClient from './BaseClient';
import Drop from './drop/Drop';
import ExportDropConfig from './drop/ExportDropConfig';
import PageRequestResults from "./PageRequestResults";
import PollableTask from "./entity/PollableTask";

class PollableTaskClient extends BaseClient {

    /**
     *
     * @param {Number} pollableId
     * @param {Number} timeout
     * @return {Promise}
     */
    waitForPollableTaskToFinish(pollableId, timeout) {
        function delay(ms) {
            return new Promise((resolve, reject) => setTimeout(resolve, ms));
        }

        // TODO timeout

        return this.get(this.getUrl(pollableId), {}).then((json) => {
            let pollableTask = PollableTask.toPollableTask(json);

            if (!pollableTask.isAllFinished) {
                return delay(500).then(this.waitForPollableTaskToFinish.bind(this, pollableId, timeout));
            }
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



