import React from "react";
import PropTypes from 'prop-types';
import {Glyphicon, Image, Modal} from "react-bootstrap";
import keycode from "keycode";

class DashboardScreenshotViewerModal extends React.Component {

    static propTypes = {
        "show": PropTypes.bool.isRequired,
        "number": PropTypes.number.isRequired,
        "total": PropTypes.number.isRequired,
        "onClose": PropTypes.func.isRequired,
        "onGoToPrevious": PropTypes.func.isRequired,
        "onGoToNext": PropTypes.func.isRequired
    }

    componentDidMount() {
        this.addWindowKeyUpDownListener();
    }

    componentWillUnmount() {
        this.removeWindowKeyDownEventListener();
    }

    addWindowKeyUpDownListener() {
        this.keydownEventListener = this.onWindowKeyDown.bind(this);
        window.addEventListener('keydown', this.keydownEventListener);
    }

    removeWindowKeyDownEventListener() {
        window.removeEventListener('keydown', this.keydownEventListener);
    }

    /**
     * Handle keyboard event to allow screenshots navigation
     *
     * @param {SynteticEvent} e
     * @returns {undefined}
     */
    onWindowKeyDown(e) {
        // TODO(ja) should event listening done in the container component??

        // react to keyboard event only if shown
        if (this.props.show) {
            switch (keycode(e)) {
                case "left":
                    this.props.onGoToPrevious();
                    break;
                case "right":
                    this.props.onGoToNext();
                    break;
            }
        }
    }

    render() {
        return (
            <Modal show={this.props.show} onHide={this.props.onClose}
                   dialogClassName="dashboard-screenshotviewer-modal">
                <Modal.Header closeButton>
                    <Modal.Title>
                        {this.props.number} / {this.props.total}
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div>
                        <Image src="api/images/c28ed1b8-5f06-4894-973e-6110d438a875screen1.png"
                               responsive
                               className="dashboard-screenshotviewer-modal-image"/>
                    </div>

                    <span className="dashboard-screenshotviewer-modal-gotoprevious" onClick={this.props.onGoToPrevious}>
                        <Glyphicon glyph="menu-left"/>
                    </span>

                    <span className="dashboard-screenshotviewer-modal-gotonext" onClick={this.props.onGoToNext}>
                        <Glyphicon glyph="menu-right"/>
                    </span>

                </Modal.Body>
            </Modal>
        )
    }
}

export default DashboardScreenshotViewerModal;