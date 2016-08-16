import StatusFilter from "../entity/StatusFilter";

export default class CancelDropConfig {
    /**
     * @param {Number} repoId
     * @param {Number} dropId
     * @param {PollableTask} pollableTask
     */
    constructor(repoId, dropId, pollableTask) {
        /** @type {Number} */
        this.repositoryId = repoId;

        /** @type {Number} */
        this.dropId = dropId;

        // @NOTE null for now because we don't need to support forcing status on import yet.
        this.importStatus = null;

        /** @type {PollableTask} */
        this.pollableTask = pollableTask;
    }
}
