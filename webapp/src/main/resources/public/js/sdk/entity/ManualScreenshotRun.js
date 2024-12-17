export default class ManualScreenshotRun {
    constructor() {
        this.id = null;
    }

    static toManualScreenshotRun(json) {
        const result = new ManualScreenshotRun();

        if (json) {
            result.id = json.id;
        }
        return result;
    }

}