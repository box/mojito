/**
 * Render the icon for the statuses that the backend returns.
 */

import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from 'react-intl';
import {Glyphicon} from "react-bootstrap";
import TextUnitSDK from "../../sdk/TextUnit";

class StatusGlyph extends React.Component {
    static propTypes = {
        "status": PropTypes.oneOf([
            TextUnitSDK.STATUS.APPROVED,
            TextUnitSDK.STATUS.REVIEW_NEEDED,
            TextUnitSDK.STATUS.TRANSLATION_NEEDED,
            TextUnitSDK.STATUS.REJECTED]).isRequired,
        "onClick": PropTypes.func.isRequired
    };

    getGlyphTypeAndTitle = (type) => {

        let glyph = {};

        switch (type) {
            case TextUnitSDK.STATUS.APPROVED:
                glyph = {type: 'ok', title: this.props.intl.formatMessage({id: "textUnit.reviewModal.accepted"})};
                break;
            case TextUnitSDK.STATUS.REVIEW_NEEDED:
                glyph = {type: 'eye-open', title: this.props.intl.formatMessage({id: "textUnit.reviewModal.needsReview"})};
                break;
            case TextUnitSDK.STATUS.TRANSLATION_NEEDED:
                glyph = {type: 'edit', title: this.props.intl.formatMessage({id: "textUnit.reviewModal.translationNeeded"})};
                break;
            case TextUnitSDK.STATUS.REJECTED:
                glyph = {type: 'alert', title: this.props.intl.formatMessage({id: "textUnit.reviewModal.rejected"})};
                break;
        }

        return glyph;
    }

    /**
     * @return {JSX}
     */
    render() {
        const glyph = this.getGlyphTypeAndTitle(this.props.status);
        return (<Glyphicon
                    glyph={glyph.type}
                    title={glyph.title}
                    onClick={this.props.onClick}
                    className="btn"
               />);
    }
}

export default injectIntl(StatusGlyph);
