import PropTypes from 'prop-types';
import React from "react";
import {injectIntl} from 'react-intl';
import {Button, Glyphicon, OverlayTrigger, Tooltip} from "react-bootstrap";

class ShareSearchParamsButton extends React.Component {
    static propTypes = {
        "onClick": PropTypes.func.isRequired,
        "intl": PropTypes.object.isRequired,
    };

    /**
     * @return {JSX}
     */
    render() {
        const {intl} = this.props;
        const label = intl.formatMessage({id: "workbench.shareSearchParams.button"});

        return (
            <OverlayTrigger trigger={['hover']} placement="bottom" overlay={<Tooltip id="workbench-share-tooltip">{label}</Tooltip>}>
                <Button onClick={this.props.onClick} className="mlm" title={label}>
                    <Glyphicon glyph='glyphicon glyphicon-link'/>
                </Button>
            </OverlayTrigger>
        );
    }
}

export default injectIntl(ShareSearchParamsButton);
