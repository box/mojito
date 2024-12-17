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
        this.supportedImageExtensionsSet = new Set([".png", ".gif", ".tiff", ".jpg",".jpeg"]);
    }

    setDefaultState() {
        this.show = false;
        this.uploadDisabled = true;

        this.uploadInProgress = false;

        this.fileToUpload = null;

        // Data URL
        this.imageForPreview = null;

        // Array Buffer
        this.imageForUpload = null;

        this.screenshotSrc = null;

        this.errorMessage = null;
    }

    openWithBranch() {
        this.show = true;
    }

    close() {
        this.setDefaultState();
    }

    uploadScreenshotImage() {
        if (this.isImageExtensionSupported(this.fileToUpload.name)) {
            const generatedUuid = v4() + this.fileToUpload.name;
            this.screenshotSrc = 'api/images/' + generatedUuid;
            this.uploadInProgress = true;
            this.getInstance().performUploadScreenshotImage(generatedUuid);
        } else {
            this.errorMessage =  this.fileToUpload.name + " is not in .png, .gif, .tiff, .jpg or .jpeg format.";
        }
    }

    uploadScreenshotImageSuccess() {
        this.getInstance().performUploadScreenshot();
    }

    uploadScreenshotImageError() {
        this.uploadInProgress = false;
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

    isImageExtensionSupported() {
        const fileExtension = this.fileToUpload.name.substring(this.fileToUpload.name.lastIndexOf(".")).toLowerCase();
        return this.supportedImageExtensionsSet.has(fileExtension);
    }

    loadImage() {
      if (this.fileToUpload) {

            const readerPreview = new FileReader();
            const readerUpload = new FileReader();

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
