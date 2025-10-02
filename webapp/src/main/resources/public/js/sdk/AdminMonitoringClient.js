import BaseClient from "./BaseClient";

class AdminMonitoringClient extends BaseClient {
    getEntityName() {
        return "monitoring";
    }

    getDbLatency(params) {
        return this.get(this.getUrl("db"), params);
    }
}

export default new AdminMonitoringClient();
