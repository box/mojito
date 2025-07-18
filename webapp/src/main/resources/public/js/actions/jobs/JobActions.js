import alt from "../../alt.js";

class JobActions {
    constructor() {
        this.generateActions(
            "createJob",
            "createJobSuccess",
            "createJobError",
            "updateJob",
            "updateJobSuccess",
            "updateJobError",
            "deleteJob",
            "deleteJobSuccess",
            "deleteJobError",
            "restoreJob",
            "restoreJobSuccess",
            "restoreJobError",
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
