import ImageClient from "../../sdk/ImageClient";
import ScreenshotClient from "../../sdk/ScreenshotClient";
import ScreenshotRun from "../../sdk/entity/ScreenshotRun";
import BranchesScreenshotUploadActions from "./BranchesScreenshotUploadActions";
import BranchesStore from "../../stores/branches/BranchesStore";
import Screenshot, {TextUnit, TmTextUnit} from "../../sdk/entity/Screenshot";
import uuidv4 from "uuid/v4";
import BranchTextUnitsStore from "../../stores/branches/BranchTextUnitsStore";

const BranchesScreenshotUploadDataSource = {

    performUploadScreenshotImage: {
        remote(state, generatedUuid) {
            return ImageClient.uploadImage(generatedUuid, state.imageForUpload);
        },
        success: BranchesScreenshotUploadActions.uploadScreenshotImageSuccess,
        error: BranchesScreenshotUploadActions.uploadScreenshotImageError
    },

    performUploadScreenshot: {
        remote(state) {
            let BranchesStoreState = BranchesStore.getState();

            let branchStatistic = BranchesStore.getSelectedBranchStatistic();
            let repository = branchStatistic.branch.repository;

            let screenshotRun =  new ScreenshotRun();
            screenshotRun.id = repository.manualScreenshotRun.id;

            let screenshot = new Screenshot();
            screenshotRun.screenshots.push(screenshot);

            screenshot.name = uuidv4();
            screenshot.src = state.screenshotSrc;
            screenshot.locale = repository.sourceLocale;
            screenshot.branch = branchStatistic.branch;

            const { branchTextUnitStatistics } = BranchTextUnitsStore.getState();
            branchTextUnitStatistics.forEach(branchTextUnitStatistic => {
                if (BranchesStoreState.selectedBranchTextUnitIds.indexOf(branchTextUnitStatistic.id) >= 0) {
                    screenshot.textUnits.push(new TextUnit(new TmTextUnit(branchTextUnitStatistic.tmTextUnit.id)));
                }
            });

            return ScreenshotClient.createOrUpdateScreenshotRun(screenshotRun);
        },
        success: BranchesScreenshotUploadActions.uploadScreenshotSuccess,
        error: BranchesScreenshotUploadActions.uploadScreenshotError
    }
};

export default BranchesScreenshotUploadDataSource;