
import Screenshot from './Screenshot'

export default class ScreenshotRun {
    constructor() {
        this.repository = null;
        this.name = null;
        this.screenshots = null;
    }

    static branchStatisticsToScreenshotRun(branchStatisticsContent, image, textUnitChecked) {
        let result = new ScreenshotRun();

        //TODO: 1) add repository to branchStatistics 2) add locale to UI

        result.id = branchStatisticsContent.branch.repository.manualScreenshotRun.id;
        result.screenshots = Screenshot.branchStatisticsContentToScreenshot(branchStatisticsContent, image, textUnitChecked);


        return result;

    }
}