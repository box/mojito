import UserStatics from "../../utils/UserStatics.js";
import Authority from "./Authority.js";

export default class User {
    constructor() {
        /** @type {Number} */
        this.id = 0;

        /** @type {String} */
        this.username = "";

        /** @type {Number} */
        this.enabled = true;

        /** @type {String} */
        this.surname = "";

        /** @type {String} */
        this.givenName = "";

        /** @type {String} */
        this.commonName = "";

        /** @type {Date} */
        this.createdDate = null;

        /** @type {Authority[]} */
        this.authorities = [];
    }

    /**
     * Get best display name for user
     */
    getDisplayName() {
        let name = this.username;

        if (this.commonName) {
            name = this.commonName;
        } else if (this.givenName && this.surname) {
            name = this.givenName + " " + this.surname;
        }

        return name;
    }

    /**
     * Get the role of the user (the first and only authority)
     *
     * @returns {String}
     */
    getRole() {
        if (this.authorities.length === 1) {
            return this.authorities[0].authority;
        }
        return UserStatics.authorityUser();
    }


    /**
     * Convert JSON User object
     *
     * @param {Object} jsonUser
     * @return {User}
     */
    static toUser(jsonUser) {
        let user = null;
        if (jsonUser) {
            user = new User();
            user.id = jsonUser.id;
            user.username = jsonUser.username;
            user.commonName = jsonUser.commonName;
            user.surname = jsonUser.surname;
            user.givenName = jsonUser.givenName;
            user.createdDate = new Date(jsonUser.createdDate);
            user.authorities = Authority.toAuthorities(jsonUser.authorities);
        }
        return user;
    }
}
