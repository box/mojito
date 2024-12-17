import _ from "lodash";
import React from "react";
import createReactClass from 'create-react-class';
import {FormattedMessage, injectIntl} from 'react-intl';
import {DropdownButton, MenuItem} from "react-bootstrap";

import ViewModeStore from "../../stores/workbench/ViewModeStore";

let ViewModeDropdown = createReactClass({
    renderModeMenuItem(mode) {
        return (
            <MenuItem eventKey={mode} active={this.props.viewMode === mode} onSelect={this.props.onModeSelected}>
                <FormattedMessage id={"search.viewMode." + mode} />
            </MenuItem>
        );
    },

    render() {
        return (
            <DropdownButton
                id="WorkbenchStatusDropdown"
                title={this.props.intl.formatMessage({id: "search.viewMode.title"}, {mode: this.props.intl.formatMessage({id: "search.viewMode." + this.props.viewMode})})}
            >
                {this.renderModeMenuItem(ViewModeStore.VIEW_MODE.FULL)}
                {this.renderModeMenuItem(ViewModeStore.VIEW_MODE.REDUCED)}
                {this.renderModeMenuItem(ViewModeStore.VIEW_MODE.STANDARD)}
                {this.renderModeMenuItem(ViewModeStore.VIEW_MODE.COMPACT)}
            </DropdownButton>
        );
    },
});


export default injectIntl(ViewModeDropdown);
