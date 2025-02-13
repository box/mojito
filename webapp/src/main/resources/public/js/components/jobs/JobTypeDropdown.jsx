import React from "react";
import createReactClass from 'create-react-class';
import {injectIntl} from 'react-intl';
import {DropdownButton, MenuItem} from "react-bootstrap";

let JobTypeDropDown = createReactClass({
    displayName: 'JobTypeDropDown',

    forceDropdownOpen: false,

    /**
     * @return {JSX.Element}
     */
    render() {
        return (
                <span className="mlm">
                    <DropdownButton id="JobTypeDropdown" disabled={true} title={"Third Party Sync"}>
                        <MenuItem divider/>
                        <MenuItem active={true}>Third Party Sync</MenuItem>
                    </DropdownButton>
                </span>
        );

    },
});

export default injectIntl(JobTypeDropDown);
