import alt from "../../alt";

class JobActions {
    constructor() {
        this.generateActions(
            "getAllJobs",
            "getAllJobsSuccess",
            "getAllJobsError",
            "triggerJob",
            "triggerJobSuccess",
            "triggerJobError",
            "disableJob",
            "disableJobSuccess",
            "enableJob",
            "enableJobSuccess",
            "setJobFilter"
        );
    }
}

export default alt.createActions(JobActions);
