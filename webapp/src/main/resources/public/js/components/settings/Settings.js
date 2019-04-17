import React from "react";
import {Nav, NavItem} from "react-bootstrap";

import BoxSettings from "./BoxSettings";

class Settings extends React.Component {
    state = {
        "currentSettingPanel": "box"
    };

    getMainContent = () => {
        let result = "";
        switch (this.state.currentSettingPanel) {
            case "box":
                result = (<BoxSettings />);
        }

        return result;
    };

    handleSelect = () => {
        // TODO add more settings
    };

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
}

export default Settings;
