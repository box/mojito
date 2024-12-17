import React from "react";
import PropTypes from 'prop-types';
import {Alert, Button, Image, Modal} from "react-bootstrap";
import {FormattedMessage, injectIntl} from "react-intl";

class BranchesScreenshotUploadModal extends React.Component {

    static propTypes = {
        "show": PropTypes.bool.isRequired,
        "uploadDisabled": PropTypes.bool.isRequired,
        "uploadInProgress": PropTypes.bool.isRequired,
        "onUpload": PropTypes.func.isRequired,
        "onCancel": PropTypes.func.isRequired,
        "onSelectedFileChange": PropTypes.func.isRequired,
        "imageForUpload": PropTypes.string,
        "imageForPreview": PropTypes.object,
        "errorMessage": PropTypes.string
    }

    render() {
        return (
            <Modal show={this.props.show} onHide={this.props.onCancel}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id="branches.screenshotUploadModal.title"/></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <form>
                        <input type="file"
                               onChange={(e) => {
                                   this.props.onSelectedFileChange(e.target.files)
                               }}
                               accept="image/*"
                        />
                    </form>

                    <div>
                        <Image src={this.props.imageForPreview} responsive/>
                    </div>

                    {this.props.errorMessage &&
                    <Alert bsStyle="danger">{this.props.errorMessage}</Alert>
                    }

                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.props.onUpload} bsStyle="primary"
                            disabled={this.props.imageForUpload === null || this.props.uploadInProgress}>
                        {!this.props.uploadInProgress && <FormattedMessage id="branches.screenshotUploadModal.upload"/>}
                        {this.props.uploadInProgress && <FormattedMessage id="upload.screenshot.processing"/>}
                    </Button>
                    <Button onClick={this.props.onCancel}>
                        <FormattedMessage id="label.cancel"/>
                    </Button>
                </Modal.Footer>
            </Modal>
        )
    }
}

export default injectIntl(BranchesScreenshotUploadModal);