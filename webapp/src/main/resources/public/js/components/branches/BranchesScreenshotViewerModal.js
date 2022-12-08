import React from "react";
import PropTypes from 'prop-types';
import {Button, Col, Glyphicon, Image, Modal, OverlayTrigger, Popover} from "react-bootstrap";
import keycode from "keycode";
import ClassNames from "classnames";
import {injectIntl} from "react-intl";

class BranchesScreenshotViewerModal extends React.Component {

    static propTypes = {
        "show": PropTypes.bool.isRequired,
        "number": PropTypes.number.isRequired,
        "total": PropTypes.number.isRequired,
        "src": PropTypes.string,
        "isDeleting": PropTypes.bool,
        "onClose": PropTypes.func.isRequired,
        "onGoToPrevious": PropTypes.func.isRequired,
        "onGoToNext": PropTypes.func.isRequired,
        "onDelete": PropTypes.func.isRequired
    }

    popover() {
        return <Popover id="remove-screenshot-popover">
            <div>Are you sure you wish to remove this screenshot?</div>
            <Button bsStyle="danger" id="remove-screenshot-confirm-btn"
                    onClick={this.props.onDelete}>
                Yes
            </Button>
        </Popover>
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

    renderTextUnit(textUnit) {
        return (<div key={textUnit.id} className="mbm">
                <div>{textUnit.tmTextUnit.name}</div>
                <div className="color-gray-light">{textUnit.tmTextUnit.content}</div>
            </div>);
    }

    render() {

        let hasPrevious = this.props.number > 1;
        let hasNext = this.props.number < this.props.total;

        return this.props.show && (<Modal show={this.props.show} onHide={this.props.onClose}
                                          dialogClassName="branches-screenshotviewer-modal">
                <Modal.Header closeButton>
                    <Modal.Title>
                        {this.props.number} / {this.props.total}
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>

                    <Col md={2} className="branches-screenshotviewer-modal-cols">
                        <div className="branches-screenshotviewer-modal-textunits-container">
                            <div className="branches-screenshotviewer-modal-textunits">
                                {this.props.textUnits.map((tu) => this.renderTextUnit(tu))}
                            </div>
                            {this.props.isDeleting ? <div>loading</div> :
                                <div className="branches-screenshotviewer-modal-delete-container">
                                    <OverlayTrigger trigger="click" placement="top" overlay={this.popover()}>
                                        <Button bsStyle="danger" style={{fontSize: 11}}>
                                            Remove
                                        </Button>
                                    </OverlayTrigger>
                                </div>
                            }
                        </div>
                    </Col>

                    <Col md={10} className="branches-screenshotviewer-modal-cols">
                        <div className="branches-screenshotviewer-modal-image-container">
                            <Image src={this.props.src}
                                   className="branches-screenshotviewer-modal-image"/>
                        </div>
                    </Col>

                    <span
                        className={ClassNames("branches-screenshotviewer-modal-gotoprevious", {"enabled": hasPrevious})}
                        onClick={this.props.onGoToPrevious}>
                        <Glyphicon glyph="menu-left"/>
                    </span>

                    <span className={ClassNames("branches-screenshotviewer-modal-gotonext", {"enabled": hasNext})}
                          onClick={this.props.onGoToNext}>
                        <Glyphicon glyph="menu-right"/>
                    </span>

                </Modal.Body>
            </Modal>)
    }
}

export default injectIntl(BranchesScreenshotViewerModal);