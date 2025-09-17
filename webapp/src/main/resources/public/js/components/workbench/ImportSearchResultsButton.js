import PropTypes from 'prop-types';
import React from "react";
import {Button, Glyphicon, Tooltip, OverlayTrigger} from "react-bootstrap";
import {injectIntl} from "react-intl";

class ImportSearchResultsButton extends React.Component {
    static propTypes = {
        "onClick": PropTypes.func.isRequired,
        "disabled": PropTypes.bool,
    };

    render() {
        const {intl} = this.props;
        const label = intl.formatMessage({id: "workbench.import.button"});

        return (
            <OverlayTrigger trigger={['hover']} placement="bottom" overlay={<Tooltip id="workbench-import-tooltip">{label}</Tooltip>}>
                <Button onClick={this.props.onClick} className="mlm" title={label} disabled={this.props.disabled}>
                    <Glyphicon glyph='upload'/>
                </Button>
            </OverlayTrigger>
        );
    }
}

export default injectIntl(ImportSearchResultsButton);

