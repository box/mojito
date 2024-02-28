import BaseClient from "./BaseClient";
import User from "./entity/User";
import UserPage from "./UsersPage";

class UserClient extends BaseClient {

    /**
     * @param {Number} page
     * @param {Number} size
     * @returns {UserPage}
     */
    getUsers(page = 0, size = 5) {
        let promise = this.get(this.getUrl(), {
            "page": page,
            "size": size
        });

        return promise.then(function (result) {
            return UserPage.toUserPage(result);
        });
    }

    checkUsernameTaken(username) {
        let promise = this.get(this.getUrl(), {
            "username": username,
            "page": 0,
            "size": 10
        });

        return promise.then(function (result) {
            return UserPage.toUserPage(result).totalElements > 0;
        });
    }

    deleteUser(id) {
        return this.delete(this.getUrl(id));
    }

    saveNewUser(user) {
        return this.post(this.getUrl(), user);
    }

    saveEditUser(parmas) {
        return this.patch(this.getUrl(parmas.id), parmas.user);
    }

    getEntityName() {
        return 'users';
    }
}

export default new UserClient();
