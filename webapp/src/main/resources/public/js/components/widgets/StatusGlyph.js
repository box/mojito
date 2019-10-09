/**
 * Render the icon for the statuses that the backend returns.
 */

import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from 'react-intl';
import {Glyphicon} from "react-bootstrap";
import TextUnitSDK from "../../sdk/TextUnit";

const glyphMap = {
    [TextUnitSDK.STATUS.APPROVED]: 'ok',
    [TextUnitSDK.STATUS.REVIEW_NEEDED]: 'eye-open',
    [TextUnitSDK.STATUS.TRANSLATION_NEEDED]: 'edit'
};

class StatusGlyph extends React.Component {
    static propTypes = {
        "status": PropTypes.oneOf([
            TextUnitSDK.STATUS.APPROVED,
            TextUnitSDK.STATUS.REVIEW_NEEDED,
            TextUnitSDK.STATUS.TRANSLATION_NEEDED]).isRequired,
        "onClick": PropTypes.func.isRequired
    };

    getGlyph = (type) => {
        return glyphMap[type] || glyphMap.TRANSLATION_NEEDED;
    };

    /**
     * @return {JSX}
     */
    render() {
        return (<Glyphicon 
                    glyph={this.getGlyph(this.props.status)} 
                    title={this.props.status} 
                    onClick={this.props.onClick}
                    className="btn"
               />);
    }
}

export default injectIntl(StatusGlyph);
