import StatusFilter from "../entity/StatusFilter";

export default class CancelDropConfig {
    /**
     * @param {Number} dropId
     * @param {PollableTask} pollableTask
     */
    constructor(dropId, pollableTask) {
        /** @type {Number} */
        this.dropId = dropId;

        /** @type {PollableTask} */
        this.pollableTask = pollableTask;
    }
}
