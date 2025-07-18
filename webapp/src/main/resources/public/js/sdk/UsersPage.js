import User from "./entity/User.js";

export default class UserPage {
    constructor() {
        /** @type {User[]} */
        this.content = [];

        /** @type {Boolean} */
        this.first = false;

        /** @type {Boolean} */
        this.last = false;

        /** @type {Boolean} */
        this.empty = false;

        /** @type {Number} */
        this.totalElements = 0;

        /** @type {Number} */
        this.totalPages = 0;

        /** @type {Number} */
        this.size = 0;

        /** @type {Number} */
        this.number = 0;
    }

    /**
     * Convert JSON UserPage object
     *
     * @param {Object} jsonUserPage
     * @return {UserPage}
     */
    static toUserPage(jsonUserPage) {
        let userPage = null;
        if (jsonUserPage) {
            userPage = new UserPage();
            userPage.content = [];
            for (const jsonUser of jsonUserPage.content) {
                userPage.content.push(User.toUser(jsonUser));
            }

            userPage.first = jsonUserPage.first;
            userPage.last = jsonUserPage.last;
            userPage.empty = jsonUserPage.empty;
            userPage.totalElements = jsonUserPage.totalElements;
            userPage.totalPages = jsonUserPage.totalPages;
            userPage.size = jsonUserPage.size;
            userPage.number = jsonUserPage.number;
        }
        return userPage;
    }
};
