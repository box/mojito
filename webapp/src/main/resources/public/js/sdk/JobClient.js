import BaseClient from "./BaseClient.js";

class JobClient extends BaseClient {
    createJob(job) {
        return this.post(this.getUrl(), job);
    }

    updateJob(job) {
        return this.patch(this.getUrl() + `/${job.id}`, job);
    }

    deleteJob(job) {
        return this.delete(this.getUrl() + `/${job.id}`);
    }

    restoreJob(job) {
        return this.patch(this.getUrl() + `/${job.id}/restore`);
    }

    getJobs() {
        return this.get(this.getUrl(), {});
    }

    triggerJob(job) {
        const jobTriggerUrl = `/${job.id}/trigger`
        return this.post(this.getUrl() + jobTriggerUrl, null);
    }

    disableJob(job) {
        const jobDisableUrl = `/${job.id}/disable`
        return this.post(this.getUrl() + jobDisableUrl, null);
    }

    enableJob(job) {
        const jobEnableUrl = `/${job.id}/enable`
        return this.post(this.getUrl() + jobEnableUrl, null);
    }

    getEntityName() {
        return 'jobs';
    }
}

export default new JobClient();