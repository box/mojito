import alt from "../../alt";
import DashboardScreenshotUploadActions from "../../actions/dashboard/DashboardScreenshotUploadActions";
import DashboardScreenshotUploadDataSource from "../../actions/dashboard/DashboardScreenshotUploadDataSource";
import v4 from "uuid/v4";
import DashboardPageActions from "../../actions/dashboard/DashboardPageActions";

class DashboardScreenshotUploadModalStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(DashboardScreenshotUploadActions);
        this.registerAsync(DashboardScreenshotUploadDataSource);
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
        console.log("uploadScreenshotImageError error"); //TODO(ja)
    }

    uploadScreenshotSuccess() {
        this.close();
        setTimeout(() => {
            //TODO(ja) ??
            DashboardPageActions.getBranches();
        }, 1);
    }

    uploadScreenshotError() {
        console.log("uploadScreenshotError error"); //TODO(ja)
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
                DashboardScreenshotUploadActions.changeImageForPreview(readerPreview.result);
            };

            readerUpload.onloadend = () => {
                DashboardScreenshotUploadActions.changeImageForUpload(readerUpload.result);
            };

            readerPreview.readAsDataURL(this.fileToUpload);
            readerUpload.readAsArrayBuffer(this.fileToUpload);
        } else {
            this.imageForPreview = null;
            this.imageForUpload = null;
        }
    }
}

export default alt.createStore(DashboardScreenshotUploadModalStore, 'DashboardScreenshotUploadModalStore');
