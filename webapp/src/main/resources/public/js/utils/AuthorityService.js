import UserStatics from "./UserStatics";

class AuthorityService {
    static userHasPermission(componentName) {
        let authorityLevel = this.componentName2AuthorityLevel(componentName);
        return authorityLevel.includes(APP_CONFIG.user.role);
    }

    static componentName2AuthorityLevel(componentName){
        let admin = UserStatics.authorityAdmin();
        let pm = UserStatics.authorityPm();
        let translator = UserStatics.authorityTranslator();
        let user = UserStatics.authorityUser();

        let level=[];

        switch (componentName) {
            case "edit-screenshots":
            case "project-requests":
            case "user-management":
                level.push(admin, pm);
                break;
            case "edit-translations":
                level.push(translator, admin, pm);
                break;
        }

        return level;
    }

    static canEditTransalationRoles() {
        return this.componentName2AuthorityLevel("edit-translations");
    }

    static canViewUserManagement() {
        return this.userHasPermission("user-management");
    }

    static canEditProjectRequests() {
        return this.userHasPermission("project-requests");
    }

    static canEditTranslations() {
        return this.userHasPermission("edit-translations");
    }

    static canEditScreenshots() {
        return this.userHasPermission("edit-screenshots");
    }

    static isAdmin() {
        return APP_CONFIG.user.role === UserStatics.authorityAdmin();
    }

    static canTranslateLocale(tag) {
        if (!this.canEditTranslations()) {
            return false;
        }
        if (APP_CONFIG.user.canTranslateAllLocales) {
            return true;
        }
        return APP_CONFIG.user.userLocales.includes(tag);
    }
}

export default AuthorityService;
