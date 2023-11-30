import PropTypes from 'prop-types';
import React from "react";
import {injectIntl} from 'react-intl';
import {Button, Glyphicon} from "react-bootstrap";

class StatusGlyph extends React.Component {
    static propTypes = {
        "onClick": PropTypes.func.isRequired
    };

    /**
     * @return {JSX}
     */
    render() {
        return (
            <Button onClick={this.props.onClick} className="mlm">
                <Glyphicon glyph='glyphicon glyphicon-link'/>
            </Button>
        );
    }
}

export default injectIntl(StatusGlyph);
