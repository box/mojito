import PropTypes from 'prop-types';
import React from "react";
import {Button, Glyphicon, Tooltip, OverlayTrigger} from "react-bootstrap";
import {injectIntl} from "react-intl";

class ExportSearchResultsButton extends React.Component {
    static propTypes = {
        "onClick": PropTypes.func.isRequired,
        "disabled": PropTypes.bool,
    };

    render() {
        const {intl} = this.props;
        const label = intl.formatMessage({id: "workbench.export.button"});

        return (
            <OverlayTrigger placement="bottom" overlay={<Tooltip id="workbench-export-tooltip">{label}</Tooltip>}>
                <Button onClick={this.props.onClick} className="mlm" title={label} disabled={this.props.disabled}>
                    <Glyphicon glyph='download-alt'/>
                </Button>
            </OverlayTrigger>
        );
    }
}

export default injectIntl(ExportSearchResultsButton);
