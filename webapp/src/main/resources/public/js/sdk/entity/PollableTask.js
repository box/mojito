import User from "./User.js";

export default class PollableTask {
    constructor() {

        /** @type {Number} */
        this.id = 0;

        /** @type {String} */
        this.name = "";

        /** @type {Date} */
        this.finishedDate = null;

        /** @type {String} */
        this.message = "";

        /** @type {String} */
        this.errorMessage = "";

        /** @type {String} */
        this.errorStack = "";

        /** @type {Number} */
        this.expectedSubTaskNumber = 0;

        /** @type {PollableTask[]} */
        this.subTasks = [];

        /** @type {PollableTask} */
        this.parentTask = null;

        /** @type {Number} */
        this.timeout = 0;

        /** @type {User} */
        this.createdByUser = null;

        /** @type {Boolean} */
        this.isAllFinished = false;
    }

    /**
     * Convert JSON User object
     *
     * @param {Object} json
     * @return {PollableTask}
     */
    static toPollableTask(json) {
        let result = null;

        if (json) {
            result = new PollableTask();

            result.id = json.id;

            result.name = json.name;

            result.finishedDate = new Date(json.finishedDate);

            result.message = json.message;

            if (json.errorMessage) {
                result.errorMessage = json.errorMessage;
                result.errorStack = json.errorStack;
            }

            result.expectedSubTaskNumber = json.expectedSubTaskNumber;
            result.subTasks = PollableTask.toPollableTasks(json.subTasks);
            result.parentTask = PollableTask.toPollableTask(json.parentTask);
            result.timeout = json.timeout;
            result.createdByUser = User.toUser(json.createdByUser);

            // NOTE PollableTask.isAllFinished json property is just "allFinished"
            result.isAllFinished = json.allFinished;
        }

        return result;
    }

    /**
     * @param {Object[]} jsons
     * @return {PollableTask[]}
     */
    static toPollableTasks(jsons) {
        let results = null;
        if (jsons) {
            results = [];
            for (const json of jsons) {
                results.push(PollableTask.toPollableTask(json));
            }
        }
        return results;
    }
}
