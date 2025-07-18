import UserStatics from "./UserStatics.js";

class AuthorityService {
    static userHasPermission(componentName) {
        const authorityLevel = this.componentName2AuthorityLevel(componentName);
        return authorityLevel.includes(APP_CONFIG.user.role);
    }

    static componentName2AuthorityLevel(componentName){
        const admin = UserStatics.authorityAdmin();
        const pm = UserStatics.authorityPm();
        const translator = UserStatics.authorityTranslator();

        const level=[];

        switch (componentName) {
            case "trigger-enable-disable-jobs":
            case "edit-screenshots":
            case "project-requests":
            case "user-management":
                level.push(admin, pm);
                break;
            case "edit-translations":
                level.push(translator, admin, pm);
                break;
            case "delete-restore-jobs":
                level.push(admin);
                break;
        }

        return level;
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

    static canTriggerEnableDisableJobs() {
        return this.userHasPermission("trigger-enable-disable-jobs");
    }

    static canDeleteRestoreJobs() {
        return this.userHasPermission("delete-restore-jobs");
    }
}

export default AuthorityService;
