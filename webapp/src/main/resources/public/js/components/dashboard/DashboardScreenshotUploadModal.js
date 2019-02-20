import React from "react";
import PropTypes from 'prop-types';
import {Button, Image, Modal} from "react-bootstrap";
import {FormattedMessage} from "react-intl";

class DashboardScreenshotUploadModal extends React.Component {

    static propTypes = {
        "show": PropTypes.bool.isRequired,
        "uploadDisabled": PropTypes.bool.isRequired,
        "onUpload": PropTypes.func.isRequired,
        "onCancel": PropTypes.func.isRequired,
        "onSelectedFileChange": PropTypes.func.isRequired,
        "imageForUpload": PropTypes.string,
        "imageForPreview": PropTypes.object
    }

    render() {
        return (
            <Modal show={this.props.show} onHide={this.props.closeModal}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id="dashboard.screenshotUploadModal.title"/></Modal.Title>
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

                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.props.onUpload} bsStyle="primary"
                            disabled={this.props.imageForUpload === null}>
                        <FormattedMessage id="dashboard.screenshotUploadModal.upload"/>
                    </Button>
                    <Button onClick={this.props.onCancel}>
                        <FormattedMessage id="label.cancel"/>
                    </Button>
                </Modal.Footer>
            </Modal>
        )
    }
}

export default DashboardScreenshotUploadModal;