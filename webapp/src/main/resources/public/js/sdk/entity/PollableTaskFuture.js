import PollableTask from "./PollableTask";

export default class PollableTaskFuture {
    constructor() {
        /** @type {Boolean} */
        this.cancelled = false;

        /** @type {Boolean} */
        this.done = false;

        /** @type {PollableTask} */
        this.pollableTask = null;
    }

    /**
     * Convert JSON object
     *
     * @param {Object} json
     * @return {PollableTaskFuture}
     */
    static toPollableTaskFuture(json) {
        let result = null;

        if (json) {
            result = new PollableTaskFuture();
            result.cancelled = json.cancelled;
            result.done = json.done;
            result.pollableTask = PollableTask.toPollableTask(json.pollableTask);
        }

        return result;
    }

    /**
     * @param {Object[]} jsons
     * @return {PollableTaskFuture[]}
     */
    static toPollableTaskFutures(jsons) {
        let results = null;
        if (jsons) {
            results = [];
            for (const json of jsons) {
                results.push(PollableTaskFuture.toPollableTaskFuture(json));
            }
        }
        return results;
    }
}
