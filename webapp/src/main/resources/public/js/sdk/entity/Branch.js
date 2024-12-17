import User from "./User";
import BranchRepository from "./BranchRepository";
import BranchStatisticScreenshot from "./BranchStatisticScreenshot";

export default class Branch {
    constructor() {
        /**
         *
         * @type {number}
         */
        this.id = null;
        /**
         *
         * @type {String}
         */
        this.name = null;

        this.repository = null;

        /**
         *
         * @type {User}
         */
        this.createdByUser = null;

        /** @type {Date} */
        this.createdDate = null;

        /**
         *
         * @type {Boolean}
         */
        this.deleted = null;

        this.screenshots = [];

    }

    static toBranch(json) {
        const result = new Branch();

        if (json) {
            result.id = json.id;
            result.name = json.name;
            result.repository = BranchRepository.toBranchRepository(json.repository);
            result.createdByUser = User.toUser(json.createdByUser);
            result.createdDate = new Date(json.createdDate);
            result.deleted = json.deleted;
            result.screenshots = BranchStatisticScreenshot.toBranchStatisticScreenshotList(json.screenshots);
        }

        return result;
    }
}