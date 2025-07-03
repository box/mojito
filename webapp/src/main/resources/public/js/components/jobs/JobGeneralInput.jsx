import React from "react";
import JobRepositoryDropDown from "./JobRepositoryDropDown";
import JobTypeDropdown from "./JobTypeDropdown";
import CronExpressionInput from "./CronExpressionInput";

const JobGeneralInput = ({ selectedRepository, onRepositorySelect, jobType, onJobTypeChange, cron, onInputChange }) => (
    <div>
        <div className="form-group mbm">
            <label>Repository*</label>
            <JobRepositoryDropDown
                selectedRepository={selectedRepository}
                onSelect={onRepositorySelect}
            />
        </div>
        <div className="form-group mbm">
            <label>Job Type*</label>
            <JobTypeDropdown jobType={jobType} onJobTypeChange={onJobTypeChange} />
        </div>
        <div className="form-group mbm">
            <label>Sync Frequency (Cron)*</label>
            <CronExpressionInput cron={cron} onChange={onInputChange} />
        </div>
    </div>
);

export default JobGeneralInput;
