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
            case "user-management":
                level.push(admin, pm);
                break;
        }

        return level;
    }

    static hasPermissionsForUserManagement() {
        return this.userHasPermission("user-management");
    }
}

export default AuthorityService;
