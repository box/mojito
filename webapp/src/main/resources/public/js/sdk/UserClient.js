import BaseClient from "./BaseClient";
import UserPage from "./UsersPage";

class UserClient extends BaseClient {
    getUsers(userSearcherParameters) {
        return this.get(this.getUrl(), userSearcherParameters.getParams());
    }

    checkUsernameTaken(username) {
        const promise = this.get(this.getUrl(), {
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

    updatePassword(currentPassword, newPassword) {
        return this.post(this.getUrl() + "/pw", {
            currentPassword: currentPassword,
            newPassword: newPassword,
        });
    }

    getEntityName() {
        return 'users';
    }
}

export default new UserClient();
