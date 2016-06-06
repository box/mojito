import React from "react";
import ReactIntl from 'react-intl';
import {DropdownButton, MenuItem} from "react-bootstrap";

import WorkbenchActions from "../../actions/workbench/WorkbenchActions";

let {IntlMixin} = ReactIntl;
let {FormattedMessage} = ReactIntl;

let TextUnitSelectorCheckBox = React.createClass({

    mixins: [IntlMixin],

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
        switch(selection) {
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
            <FormattedMessage numberOfSelectedTextUnits={numberOfSelectedTextUnits}
                message={this.getIntlMessage("workbench.toolbar.numberOfSelectedTextUnits")} />
        );
    },

    render() {
        return (
            <DropdownButton title={this.getTitle()} className="mrl" onSelect={this.selectionChanged}>
                <MenuItem eventKey={this.SELECT_ALL_IN_PAGE}>{this.getIntlMessage("workbench.toolbar.selectAllInPage")}</MenuItem>
                <MenuItem eventKey={this.CLEAR_ALL_IN_PAGE}>{this.getIntlMessage("workbench.toolbar.clearAllInPage")}</MenuItem>
                <MenuItem eventKey={this.CLEAR_ALL}>{this.getIntlMessage("workbench.toolbar.clearAll")}</MenuItem>
            </DropdownButton>
        );
    }
});

export default TextUnitSelectorCheckBox;
