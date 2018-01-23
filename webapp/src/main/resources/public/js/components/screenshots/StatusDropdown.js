import keycode from "keycode";
import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {DropdownButton, MenuItem} from "react-bootstrap";
import StatusCommon, {StatusCommonTypes} from "./StatusCommon";

class StatusDropdown extends React.Component {
    
    static propTypes = {
        "status":  PropTypes.oneOf([
            StatusCommonTypes.ALL,
            StatusCommonTypes.ACCEPTED,
            StatusCommonTypes.NEEDS_REVIEW,
            StatusCommonTypes.REJECTED]).isRequired,
        "onStatusChanged": PropTypes.func.isRequired
    }

    getMessageForStatus(status) {
        switch (status) {
            case StatusCommonTypes.ALL:
                return this.props.intl.formatMessage({ id: "screenshots.statusDropdown.all" });
            case StatusCommonTypes.ACCEPTED:
                return this.props.intl.formatMessage({ id: "screenshots.statusDropdown.accepted" });
            case StatusCommonTypes.NEEDS_REVIEW:
                return this.props.intl.formatMessage({ id: "screenshots.statusDropdown.needsReview" });
            case StatusCommonTypes.REJECTED:
                return this.props.intl.formatMessage({ id: "screenshots.statusDropdown.rejected" });
        }
    }

    renderStatusMenuItem(status) {
        return (
            <MenuItem eventKey={status} 
                      active={this.props.status === status}
                      onSelect={(status) => this.props.onStatusChanged(status)} >
                {this.getMessageForStatus(status)}
            </MenuItem>
        );
    }

    render() {
        return (
            <DropdownButton id="screenshotStatusDropdown" 
                            title={this.props.intl.formatMessage({ id: "screenshots.statusDropdown.title" })}>

                <MenuItem header><FormattedMessage id="screenshots.statusDropdown.status" /></MenuItem>
                    {this.renderStatusMenuItem(StatusCommonTypes.ALL)}
                    {this.renderStatusMenuItem(StatusCommonTypes.ACCEPTED)}
                    {this.renderStatusMenuItem(StatusCommonTypes.NEEDS_REVIEW)}
                    {this.renderStatusMenuItem(StatusCommonTypes.REJECTED)}
            </DropdownButton>
        );
    }
};

export default injectIntl(StatusDropdown);
