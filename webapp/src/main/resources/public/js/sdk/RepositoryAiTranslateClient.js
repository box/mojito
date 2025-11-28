import BaseClient from "./BaseClient";

class RepositoryAiTranslateClient extends BaseClient {
    translateRepository(request) {
        return this.post(this.getUrl(), request);
    }

    getReport(pollableTaskId) {
        return this.get(this.getUrl(`report/${pollableTaskId}`), {});
    }

    getReportLocale(filename) {
        return this.get(this.getUrl(`report/${filename}`), {});
    }

    getEntityName() {
        return "proto-ai-translate";
    }
}

export default new RepositoryAiTranslateClient();
