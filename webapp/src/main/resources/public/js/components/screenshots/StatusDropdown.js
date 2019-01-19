import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {DropdownButton, MenuItem} from "react-bootstrap";
import {StatusCommonTypes} from "./StatusCommon";
import ScreenshotsSearchTextStore from "../../stores/screenshots/ScreenshotsSearchTextStore";

class StatusDropdown extends React.Component {
    
    static propTypes = {
        "status":  PropTypes.oneOf([
            StatusCommonTypes.ALL,
            StatusCommonTypes.ACCEPTED,
            StatusCommonTypes.NEEDS_REVIEW,
            StatusCommonTypes.REJECTED]).isRequired,
        "screenshotRunType":  PropTypes.oneOf([
            ScreenshotsSearchTextStore.SEARCH_SCREENSHOTRUN_TYPES.LAST_SUCCESSFUL_RUN,
            ScreenshotsSearchTextStore.SEARCH_SCREENSHOTRUN_TYPES.MANUAL_RUN]).isRequired,
        "onStatusChanged": PropTypes.func.isRequired,
        "onScreenshotRunTypeChanged": PropTypes.func.isRequired
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

    getMessageForType(type) {
        switch (type) {
            case ScreenshotsSearchTextStore.SEARCH_SCREENSHOTRUN_TYPES.LAST_SUCCESSFUL_RUN:
                return this.props.intl.formatMessage({ id: "screenshots.statusDropdown.screenshotRunType.lastSuccessful"})
            case ScreenshotsSearchTextStore.SEARCH_SCREENSHOTRUN_TYPES.MANUAL_RUN:
                return this.props.intl.formatMessage({ id: "screenshots.statusDropdown.screenshotRunType.manualRun"})
        }
    }

    renderScreenshotRunTypeMenuItem(screenshotRunType) {
        return (
            <MenuItem eventKey={screenshotRunType}
                      active={this.props.screenshotRunType === screenshotRunType}
                      onSelect={(screenshotRunType) => this.props.onScreenshotRunTypeChanged(screenshotRunType)} >
                {this.getMessageForType(screenshotRunType)}
            </MenuItem>
        );
    }

    render() {
        console.log("prop screenshotRunType", this.props.screenshotRunType);
        return (
            <DropdownButton id="screenshotStatusDropdown" 
                            title={this.props.intl.formatMessage({ id: "screenshots.statusDropdown.title" })}>

                <MenuItem header><FormattedMessage id="screenshots.statusDropdown.status" /></MenuItem>
                    {this.renderStatusMenuItem(StatusCommonTypes.ALL)}
                    {this.renderStatusMenuItem(StatusCommonTypes.ACCEPTED)}
                    {this.renderStatusMenuItem(StatusCommonTypes.NEEDS_REVIEW)}
                    {this.renderStatusMenuItem(StatusCommonTypes.REJECTED)}
                <MenuItem divider/>
                <MenuItem header><FormattedMessage id="screenshots.statusDropdown.screenshotRunType" /></MenuItem>
                    {this.renderScreenshotRunTypeMenuItem(ScreenshotsSearchTextStore.SEARCH_SCREENSHOTRUN_TYPES.LAST_SUCCESSFUL_RUN)}
                    {this.renderScreenshotRunTypeMenuItem(ScreenshotsSearchTextStore.SEARCH_SCREENSHOTRUN_TYPES.MANUAL_RUN)}

            </DropdownButton>
        );
    }
};

export default injectIntl(StatusDropdown);
