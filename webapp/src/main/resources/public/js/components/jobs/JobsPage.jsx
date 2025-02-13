import React from "react";
import createReactClass from 'create-react-class';
import {withRouter} from "react-router";
import JobTypeDropdown from "./JobTypeDropdown";
import JobsView from "./JobsView";
import AltContainer from "alt-container";
import JobStore from "../../stores/jobs/JobStore";
import RepositoryDropDown from "./RepositoryDropDown";

let JobsPage = createReactClass({
    displayName: 'JobsPage',

    render: function () {
        const clearLeftFix = {
            clear: 'left',
        };
        return (
            <div>
                <div className="pull-left">
                    <JobTypeDropdown />
                    <RepositoryDropDown />
                </div>

                <div style={clearLeftFix}></div>

                <AltContainer store={JobStore} className="mtl mbl" >
                    <JobsView />
                </AltContainer>
            </div>
        );
    },
});

export default withRouter(JobsPage);
