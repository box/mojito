import JobClient from "../../sdk/JobClient.js";
import JobActions from "./JobActions.js";

const JobDataSource = {
    createJob: {
        remote(state, job) {
            return JobClient.createJob(job);
        },

        success: JobActions.createJobSuccess,
        error: JobActions.createJobError
    },
    updateJob: {
        remote(state, job) {
            return JobClient.updateJob(job);
        },

        success: JobActions.updateJobSuccess,
        error: JobActions.updateJobError
    },
    deleteJob: {
        remote(state, job) {
            return JobClient.deleteJob(job);
        },

        success: JobActions.deleteJobSuccess,
        error: JobActions.deleteJobError
    },
    restoreJob: {
        remote(state, job) {
            return JobClient.restoreJob(job);
        },
        success: JobActions.restoreJobSuccess,
        error: JobActions.restoreJobError
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
