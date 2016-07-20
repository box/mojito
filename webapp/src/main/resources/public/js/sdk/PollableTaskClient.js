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
        return this.get(this.getUrl(pollableId), {}).then((result) => {
            let pollableTask = PollableTask.toPollableTask(result);

            // TODO timeout
            // TODO settimeout promise, wait
            return pollableTask.isAllFinished ? true : this.waitForPollableTaskToFinish(pollableId, timeout);
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



