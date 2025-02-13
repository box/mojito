import BaseClient from "./BaseClient";

class JobClient extends BaseClient {

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