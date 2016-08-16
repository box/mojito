import React from "react";
import {FormattedMessage} from "react-intl";
import {DropdownButton, MenuItem} from "react-bootstrap";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";

let TextUnitSelectorCheckBox = React.createClass({

    componentWillMount() {
        // TODO: Move these to SearchConstants file. componentWillMount is not the place to create constants.
        this.SELECT_ALL_IN_PAGE = "selectAllInPage";
        this.CLEAR_ALL_IN_PAGE = "clearSelectionsInPage";
        this.CLEAR_ALL = "clearAll";
    },

    /**
     * @param {string} selection The eventKey for the selected MenuItem in the DropdownButton
     */
    selectionChanged(selection) {
        switch (selection) {
            case this.SELECT_ALL_IN_PAGE:
                WorkbenchActions.selectAllTextUnitsInCurrentPage();
                break;
            case this.CLEAR_ALL_IN_PAGE:
                WorkbenchActions.resetSelectedTextUnitsInCurrentPage();
                break;
            case this.CLEAR_ALL:
                WorkbenchActions.resetAllSelectedTextUnits();
                break;
        }
    },

    /**
     * @returns {JSX} The JSX to display the number of selected textunits.
     */
    getTitle() {
        let numberOfSelectedTextUnits = this.props.numberOfSelectedTextUnits;

        return (
            <FormattedMessage values={{"numberOfSelectedTextUnits": numberOfSelectedTextUnits}}
                              id="workbench.toolbar.numberOfSelectedTextUnits"/>
        );
    },

    render() {
        return (
            <DropdownButton title={this.getTitle()} className="mrl" onSelect={this.selectionChanged}>
                <MenuItem eventKey={this.SELECT_ALL_IN_PAGE}><FormattedMessage id="workbench.toolbar.selectAllInPage"/></MenuItem>
                <MenuItem eventKey={this.CLEAR_ALL_IN_PAGE}><FormattedMessage
                    id="workbench.toolbar.clearAllInPage"/></MenuItem>
                <MenuItem eventKey={this.CLEAR_ALL}><FormattedMessage id="workbench.toolbar.clearAll"/></MenuItem>
            </DropdownButton>
        );
    }
});

export default TextUnitSelectorCheckBox;
