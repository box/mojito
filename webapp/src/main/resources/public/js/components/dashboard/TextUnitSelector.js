import React from "react";
import PropTypes from 'prop-types';
import {DropdownButton, MenuItem} from "react-bootstrap";
import {FormattedMessage} from "react-intl";
import DashboardStore from "../../stores/Dashboard/DashboardStore";

class TextUnitSelector extends React.Component {
    static propTypes = {
        "selectAllTextUnitsInCurrentPage": PropTypes.func.isRequired,
        "resetAllSelectedTextUnitsInCurrentPage": PropTypes.func.isRequired
    };

    getTitle() {
        return (
            <FormattedMessage values={{"numberOfSelectedTextUnits": DashboardStore.getState().numberOfTextUnitChecked}}
                              id="workbench.toolbar.numberOfSelectedTextUnits"/>
        )
    }

    render() {
        return (
            <DropdownButton id="Text" title={this.getTitle()} className="mrl" onSelect={this.selectionChanged}>
                <MenuItem onClick={this.props.selectAllTextUnitsInCurrentPage}><FormattedMessage id="workbench.toolbar.selectAllInPage"/></MenuItem>
                <MenuItem onClick={this.props.resetAllSelectedTextUnitsInCurrentPage}><FormattedMessage
                    id="workbench.toolbar.clearAllInPage"/></MenuItem>
            </DropdownButton>
        )
    }
}
export default TextUnitSelector;