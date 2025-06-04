import JobClient from "../../sdk/JobClient";
import JobActions from "./JobActions";

const JobDataSource = {
    createJob: {
        remote(state, job) {
            return JobClient.createJob(job);
        },

        success: JobActions.createJobSuccess,
        error: JobActions.createJobError
    },
    getAllJobs: {
        remote() {
            return JobClient.getJobs();
        },

        success: JobActions.getAllJobsSuccess,
        error: JobActions.getAllJobsError
    },
    triggerJob: {
        remote(state, job) {
            return JobClient.triggerJob(job);
        },
        success: JobActions.triggerJobSuccess,
        error: JobActions.triggerJobError
    },
    disableJob: {
        remote(state, job) {
            return JobClient.disableJob(job);
        },
        success: JobActions.disableJobSuccess,
        error: JobActions.disableJobError
    },
    enableJob: {
        remote(state, job) {
            return JobClient.enableJob(job);
        },
        success: JobActions.enableJobSuccess,
        error: JobActions.enableJobError
    }
};

export default JobDataSource;
