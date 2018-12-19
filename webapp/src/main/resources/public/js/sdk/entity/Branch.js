import User from "./User"
import Repository from "./Repository";
import BranchRepository from "./BranchRepository";

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

        /**
         *
         * @type {Boolean}
         */
        this.deleted = null;

    }

    static toBranch(json) {
        let result = new Branch();

        result.id = json.id;
        result.name = json.name;
        result.repository = BranchRepository.toBranchRepository(json.repository);
        result.createdByUser = User.toUser(json.createdByUser);
        result.deleted = json.deleted;

        return result;
    }
}