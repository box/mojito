import StatusFilter from "../entity/StatusFilter";

export default class ExportDropConfig {
    constructor(repoId, dropId, bcp47Tags, uploadTime, pollableTask) {
        /** @type {Number} */
        this.repositoryId = repoId;

        /** @type {Number} */
        this.dropId = dropId;

        // @NOTE named locales because of @JsonProperty com.box.l10n.mojito.service.drop.ExportDropConfig
        /** @type {String[]} */
        this.locales = bcp47Tags;

        /** @type {Date} */
        this.uploadTime = uploadTime;

        /** @type {PollableTask} */
        this.pollableTask = pollableTask;

        /** @type {StatusFilter} */
        this.type = StatusFilter.Type.Translation;
    }
}
