import React from "react";
import {Nav, NavItem} from "react-bootstrap";
import {IntlMixin} from "react-intl";

import BoxSettings from "./BoxSettings";

let Settings = React.createClass({
    mixins: [IntlMixin],

    getInitialState() {
        return {
            "currentSettingPanel": "box"
        };
    },

    getMainContent() {
        let result = "";
        switch (this.state.currentSettingPanel) {
            case "box":
                result = (<BoxSettings />);
        }

        return result;
    },

    handleSelect() {
        // TODO add more settings
    },


    render() {
        return (
            <div>
                <h4>Settings</h4>
                <Nav bsStyle="tabs" activeKey={1} onSelect={this.handleSelect}>
                    <NavItem>Box Integration</NavItem>
                </Nav>
                {this.getMainContent()}
            </div>
        );
    }

});

export default Settings;
