import React from "react";
import {Button, OverlayTrigger, Tooltip} from "react-bootstrap";
import {FormattedMessage, injectIntl} from 'react-intl';
import PropTypes from "prop-types";


class BranchesAddScreenshotButton extends React.Component {

    static propTypes = {
        "onClick": PropTypes.func.isRequired,
        "disabled": PropTypes.bool.isRequired
    };

    render() {

        // we use this construct instead of putting the button in the overlay because tooltips don't work on disabled buttons
        let button = (
            <div style={{display: "inline-block"}}>
                <Button bsStyle="primary"
                        style={this.props.disabled ? {pointerEvents: "none"} : {}}
                        bsSize="small" disabled={this.props.disabled}
                        onClick={this.props.onClick}>
                    <FormattedMessage id="branches.actions.addScreenshot"/>
                </Button>
            </div>
        );

        if (this.props.disabled) {
            button = (<OverlayTrigger placement="bottom"
                                      overlay={<Tooltip id="branches.actions.addScreenshot">
                                          <FormattedMessage id="branches.actions.addScreenshot.tooltip"/>
                                      </Tooltip>}>
                {button}
            </OverlayTrigger>);
        }

        return (
            <div className="mrl col-xs-2">{button}</div>
        );
    }
};


export default injectIntl(BranchesAddScreenshotButton);
