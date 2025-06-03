import alt from "../../alt";
import JobDataSource from "../../actions/jobs/JobDataSource";
import JobActions from "../../actions/jobs/JobActions";
import { JobStatus } from "../../utils/JobStatus";

class JobStore {

    constructor() {
        this.jobs = [];
        this.filter = [];
        this.bindActions(JobActions);
        this.registerAsync(JobDataSource);
    }

    createJob(job) {
        this.getInstance().createJob(job);
    }

    createJobSuccess(response) {
        const job = response.job;
        this.jobs = [...this.jobs, job];
    }

    deleteJob(job) {
        this.getInstance().deleteJob(job);
    }

    deleteJobSuccess(response) {
        const jobId = response.jobId;
        this.jobs = this.jobs.filter(j => j.id !== jobId);
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
