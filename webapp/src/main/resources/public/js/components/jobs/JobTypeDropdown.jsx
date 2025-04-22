import React from "react";
import createReactClass from 'create-react-class';
import {injectIntl} from 'react-intl';
import {DropdownButton, MenuItem} from "react-bootstrap";
import {JobType} from "../../utils/JobType";

let JobTypeDropDown = createReactClass({
    getInitialState() {
        return {
            jobType: JobType.THIRD_PARTY_SYNC
        };
    },

    componentDidMount() {
        this.props.onJobTypeChange(this.state.jobType);
    },

    onJobTypeChange(jobType) {
        this.setState({jobType: jobType});
        this.props.onJobTypeChange(jobType);
    },

    getJobName(jobType) {
        return jobType.toLowerCase().split("_").map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(" ");
    },

    displayName: 'JobTypeDropDown',

    forceDropdownOpen: false,

    /**
     * @return {JSX.Element}
     */
    render() {
        return (
                <span className="mlm">
                    <DropdownButton id="JobTypeDropdown" disabled={false} title={this.getJobName(this.state.jobType)}>
                        {Object.values(JobType).map(jobType => (
                            <MenuItem eventKey={jobType}
                                      key={jobType} active={jobType === this.state.jobType}
                                      onSelect={(selectedJobType, _) => this.onJobTypeChange(selectedJobType)}>
                                {this.getJobName(jobType)}
                            </MenuItem>
                        ))}
                    </DropdownButton>
                </span>
        );

    },
});

export default injectIntl(JobTypeDropDown);
