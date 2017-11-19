import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from 'react-intl';
import {Glyphicon} from "react-bootstrap";
import keymirror from "keymirror";
import StatusCommon, {StatusCommonTypes} from "./StatusCommon";

class StatusGlyph extends React.Component {
    static propTypes = {
        "status": PropTypes.oneOf([
            StatusCommonTypes.ACCEPTED,
            StatusCommonTypes.NEEDS_REVIEW,
            StatusCommonTypes.REJECTED]).isRequired,
        "onClick": PropTypes.func.isRequired
    };

    getGlyph = (type) => {
        let glyph;

        switch (type) {
            case StatusCommonTypes.ACCEPTED:
                glyph = 'ok';
                break;
            case StatusCommonTypes.NEEDS_REVIEW:
                glyph = 'eye-open';
                break;
            case StatusCommonTypes.REJECTED:
                glyph = 'alert';
                break;
        }

        return glyph;
    };

    /**
     * @return {JSX}
     */
    render() {
        return (
                <Glyphicon 
                    glyph={this.getGlyph(this.props.status)} 
                    title={StatusCommon.getScreenshotStatusIntl(this.props.intl, this.props.status)} 
                    onClick={this.props.onClick}
                    className="btn"
                    />
                );
    }
}

export default injectIntl(StatusGlyph);
