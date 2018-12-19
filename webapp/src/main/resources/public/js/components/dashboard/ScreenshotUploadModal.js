import React from "react";
import PropTypes from 'prop-types';
import {Button, ButtonGroup, ButtonToolbar, FormControl, Modal} from "react-bootstrap";
import {FormattedMessage} from "react-intl";
import ImageUpload from "./ImageUpload";

class ScreenshotUploadModal extends React.Component {
    static propTypes = {
        "showModal": PropTypes.bool.isRequired,
        "closeModal": PropTypes.func.isRequired,
        "onChooseImageClick": PropTypes.func.isRequired,
        "onUploadImageClick": PropTypes.func.isRequired
    };


    render() {
        return (
            <Modal show={this.props.showModal} onHide={this.props.closeModal}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id="dashboard.uploadScreenshotModal.title"/></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <ImageUpload onChooseImageClick={this.props.onChooseImageClick}/>
                    <Button onClick={this.props.onUploadImageClick}>
                        Sumbit
                    </Button>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.props.closeModal}>
                        <FormattedMessage id="label.cancel"/>
                    </Button>
                </Modal.Footer>
            </Modal>
        )
    }
}

export default ScreenshotUploadModal;