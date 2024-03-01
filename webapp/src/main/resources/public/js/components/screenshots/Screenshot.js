import PropTypes from 'prop-types';
import React, {Image} from "react";
import {injectIntl} from 'react-intl';
import {Label} from "react-bootstrap";
import ReactDOM from "react-dom";
import keycode from "keycode";
import StatusGlyph from "./StatusGlyph";
import {StatusCommonTypes} from "./StatusCommon";


class Screenshot extends React.Component {

    static propTypes = {
        "screenshot": PropTypes.object.isRequired,
        "onClick": PropTypes.func.isRequired,
        "isSelected": PropTypes.bool.isRequired,
        "onLocaleClick": PropTypes.func.isRequired,
        "onNameClick": PropTypes.func.isRequired,
        "onStatusChanged": PropTypes.func.isRequired,
        "onStatusGlyphClick": PropTypes.func.isRequired,
        "statusGlyphDisabled": PropTypes.bool.isRequired,
    }

    componentDidMount() {
        this.scrollIntoScreenshotIfSelected();
    }

    componentDidUpdate() {
        this.scrollIntoScreenshotIfSelected();
    }

    onLocaleClick(e) {
        e.stopPropagation();
        this.props.onLocaleClick();
    }

    onNameClick(e) {
        e.stopPropagation();
        this.props.onNameClick();
    }

    onKeyUp(e) {
        e.stopPropagation();
        switch (keycode(e)) {
            case "a":
                this.props.onStatusChanged(StatusCommonTypes.ACCEPTED);
                break;
            case "r":
                this.props.onStatusChanged(StatusCommonTypes.NEEDS_REVIEW);
                break;
            case "x":
                this.props.onStatusChanged(StatusCommonTypes.REJECTED);
                break;
        }
    }

    scrollIntoScreenshotIfSelected() {
        if (this.props.isSelected) {
            ReactDOM.findDOMNode(this.refs.screenshot).focus();
            this.refs.screenshot.scrollIntoView();
        }
    }

    /**
     * @return {JSX}
     */
    render() {

        let screenshotClassName = "screenshot";

        if (this.props.isSelected) {
           screenshotClassName += " screenshot-selected";
        }

        return (
                <div
                    ref="screenshot"
                    className={screenshotClassName}
                    onClick={this.props.onClick}
                    tabIndex={0}
                    onKeyUp={(e) => this.onKeyUp(e)}
                    >
                    <div className="screenshot-placeholder">
                        <img src={this.props.screenshot.src} />
                    </div>
                    <div className="screenshot-description">
                        <Label bsStyle='primary'
                               bsSize='large'
                               className="mrxs mtl clickable"
                               onClick={(e) => this.onLocaleClick(e)}>
                            {this.props.screenshot.locale.bcp47Tag}
                        </Label>

                        <span onClick={(e) => this.onNameClick(e)} className="clickable">
                            {this.props.screenshot.name}
                        </span>
                    </div>
                    <span className="screenshot-glyph">
                        <StatusGlyph status={this.props.screenshot.status}
                                     onClick={this.props.onStatusGlyphClick}
                                     disabled={this.props.statusGlyphDisabled}
                                    />
                    </span >
                </div>
                    );
    }
}

export default injectIntl(Screenshot);
