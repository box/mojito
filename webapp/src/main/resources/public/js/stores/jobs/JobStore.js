import alt from "../../alt";
import JobDataSource from "../../actions/jobs/JobDataSource";
import JobActions from "../../actions/jobs/JobActions";
import { JobStatus } from "../../utils/JobStatus";

class JobStore {

    constructor() {
        this.jobs = [];
        this.filter = [];
        this.error = null;
        this.bindActions(JobActions);
        this.registerAsync(JobDataSource);
    }

    createJob(job) {
        this.getInstance().createJob(job);
    }

    createJobSuccess(job) {
        this.jobs = [...this.jobs, job];
        this.setError(null);
    }

    createJobError(error) {
        this.setError(error);
    }

    updateJob(job) {
        this.getInstance().updateJob(job);
    }

    updateJobSuccess(job) {
        this.jobs = this.jobs.map(j => j.id === job.id ? { ...j, ...job } : j);
        this.setError(null);
    }

    updateJobError(error) {
        this.setError(error);
    }

    deleteJob(job) {
        this.getInstance().deleteJob(job);
    }

    deleteJobSuccess() {
        this.getAllJobs();
    }

    restoreJob(job) {
        this.getInstance().restoreJob(job);
    }

    restoreJobSuccess() {
        this.getAllJobs();
    }

    setError(message) {
        this.error = message;
    }

    getError() {
        return this.getState().error;
    }

    getAllJobs() {
        this.getInstance().getAllJobs();
    }

    getAllJobsSuccess(jobs) {
        this.jobs = jobs;
    }

    triggerJob(job) {
        this.getInstance().triggerJob(job);
    }

    triggerJobSuccess(response) {
        const jobId = response.jobId;
        // Update job on client side before the full sync (poll) occurs
        this.jobs = this.jobs.map(j => jobId === j.id ? { ...j, status: JobStatus.IN_PROGRESS, endDate: null } : j)
    }

    disableJob(job) {
        this.getInstance().disableJob(job);
    }

    disableJobSuccess(response) {
        const jobId = response.jobId;
        this.jobs = this.jobs.map(j => jobId === j.id ? { ...j, enabled: false } : j)
    }

    enableJob(job) {
        this.getInstance().enableJob(job);
    }

    enableJobSuccess(response) {
        const jobId = response.jobId;
        this.jobs = this.jobs.map(j => jobId === j.id ? { ...j, enabled: true } : j);
    }

    setJobFilter(repos) {
        this.filter = repos;
    }
}

export default alt.createStore(JobStore, 'JobStore');
