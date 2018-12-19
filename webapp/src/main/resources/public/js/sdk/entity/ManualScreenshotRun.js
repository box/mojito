export default class ManualScreenshotRun {
    constructor() {
        this.id = null;
    }

    static toManualScreenshotRun(json) {
        let result = new ManualScreenshotRun();

        result.id = json.id;

        return result;
    }

}