import React from "react";
import {Button, OverlayTrigger, Tooltip} from "react-bootstrap";
import {FormattedMessage, injectIntl} from 'react-intl';
import PropTypes from "prop-types";


class DashboardAddScreenshotButton extends React.Component {

    static propTypes = {
        "onClick": PropTypes.func.isRequired,
        "disabled": PropTypes.bool.isRequired
    };

    render() {
        var button = (
            <div style={{display: "inline-block"}}>
                <Button bsStyle="primary"
                        style={this.props.disabled ? {pointerEvents: "none"} : {}}
                        bsSize="small" disabled={this.props.disabled}
                        onClick={this.props.onClick}>
                    <FormattedMessage id="dashboard.actions.addScreenshot"/>
                </Button>
            </div>
        );

        if (this.props.disabled) {
            button = (<OverlayTrigger placement="bottom"
                                      overlay={<Tooltip id="dashboard.actions.addScreenshot">
                                          <FormattedMessage id="dashboard.actions.addScreenshot.tooltip"/>
                                      </Tooltip>}>
                {button}
            </OverlayTrigger>);
        }

        return (
            <div className="mrl col-xs-2">{button}</div>
        );
    }
};


export default injectIntl(DashboardAddScreenshotButton);
