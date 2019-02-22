import alt from "../../alt";
import BranchesScreenshotUploadActions from "../../actions/branches/BranchesScreenshotUploadActions";
import BranchesScreenshotUploadDataSource from "../../actions/branches/BranchesScreenshotUploadDataSource";
import v4 from "uuid/v4";
import BranchesPageActions from "../../actions/branches/BranchesPageActions";

class BranchesScreenshotUploadModalStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(BranchesScreenshotUploadActions);
        this.registerAsync(BranchesScreenshotUploadDataSource);
    }

    setDefaultState() {
        this.show = false;
        this.uploadDisabled = true;

        this.fileToUpload = null;

        // Data URL
        this.imageForPreview = null;

        // Array Buffer
        this.imageForUpload = null;

        this.screenshotSrc = null;

        this.errorMessage = null;
    }

    openWithBranch(branch) {
        this.show = true;
    }

    close() {
        this.setDefaultState();
    }

    uploadScreenshotImage() {
        let generatedUuid = v4() + this.fileToUpload.name;
        this.screenshotSrc = 'api/images/' + generatedUuid;
        this.getInstance().performUploadScreenshotImage(generatedUuid);
    }

    uploadScreenshotImageSuccess() {
        this.getInstance().performUploadScreenshot();
    }

    uploadScreenshotImageError() {
        this.errorMessage = "Couldn't upload image";
    }

    uploadScreenshotSuccess() {
        this.close();
        setTimeout(() => {
            BranchesPageActions.getBranches();
        }, 1);
    }

    uploadScreenshotError() {
        this.errorMessage = "Couldn't upload screenshot";
    }

    /**
     *
     * @param {FileList} files
     */
    changeSelectedFiles(files) {
        this.uploadDisabled = true;
        if (files && files.length > 0) {
            this.fileToUpload = files[0];
            this.loadImage();
        } else {
            this.fileToUpload = null;
        }
    }

    changeImageForPreview(imageForPreview) {
        this.imageForPreview = imageForPreview;
    }

    changeImageForUpload(imageForUpload) {
        this.imageForUpload = imageForUpload;
    }

    loadImage() {
      if (this.fileToUpload) {

            let readerPreview = new FileReader();
            let readerUpload = new FileReader();

            readerPreview.onloadend = () => {
                BranchesScreenshotUploadActions.changeImageForPreview(readerPreview.result);
            };

            readerUpload.onloadend = () => {
                BranchesScreenshotUploadActions.changeImageForUpload(readerUpload.result);
            };

            readerPreview.readAsDataURL(this.fileToUpload);
            readerUpload.readAsArrayBuffer(this.fileToUpload);
        } else {
            this.imageForPreview = null;
            this.imageForUpload = null;
        }
    }
}

export default alt.createStore(BranchesScreenshotUploadModalStore, 'BranchesScreenshotUploadModalStore');
