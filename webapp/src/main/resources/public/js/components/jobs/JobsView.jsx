import React from "react";
import {withAppConfig} from "../../utils/AppConfig";
import JobStore from "../../stores/jobs/JobStore";
import JobActions from "../../actions/jobs/JobActions";
import AuthorityService from "../../utils/AuthorityService";
import JobThirdPartySyncRow from "./JobThirdPartySyncRow";
import {JobType} from "../../utils/JobType";
import PropTypes from "prop-types";

class JobsView extends React.Component {
    static propTypes = {
        "jobs": PropTypes.array.isRequired,
        "filter": PropTypes.array.isRequired,
        "jobType": PropTypes.string.isRequired,
    }

    constructor(props) {
        super(props);

        const {jobs, filter, jobType} = this.props;
        this.state = {jobs, filter, jobType};
        this.isDoneInitialLoad = false;

        // Bind this to the function call to ensure 'this' references the current instance.
        this.jobStoreChange = this.jobStoreChange.bind(this);

        if(AuthorityService.canViewJobs()) {
            // Only poll if we have access (stops polling for 403 responses)
            JobActions.getAllJobs();
            this.interval = setInterval(() => {
                JobActions.getAllJobs();
            }, 5000);
        }
    }

    componentDidUpdate(prevProps) {
        if (prevProps.jobType !== this.props.jobType) {
            this.setState({...this.state, jobType: this.props.jobType});
        }
    }

    componentDidMount() {
        // Start listening to JobStore updates
        JobStore.listen(this.jobStoreChange);
    }

    componentWillUnmount() {
        // Stop listening to JobStore and clear the polling loop
        JobStore.unlisten(this.jobStoreChange);
        clearInterval(this.interval);
    }

    jobStoreChange(state) {
        // Any change to the JobStore will come here
        this.isDoneInitialLoad = true;
        this.setState({...this.state, jobs: state.jobs, filter: state.filter})
    }

    createJobRow(job, index) {
        // Render specific row with correct job information depending on the type.
        switch(job.type) {
            case JobType.THIRD_PARTY_SYNC:
                return <JobThirdPartySyncRow key={job.id} job={job} />;
            case JobType.EVOLVE_SYNC:
                return <JobThirdPartySyncRow key={job.id} job={job} hideThirdPartyLink={true} />;
            default:
                return null;
        }
    }

    /**
     * @return {XML}
     */
    render() {
        if (!AuthorityService.canViewJobs()) {
            return (
                <div className="ptl">
                    <h4 className="text-center mtl">You do not have permissions to view Jobs.</h4>
                </div>
            )
        }

        if(this.isDoneInitialLoad && this.state.jobs.length === 0)  {
            return (
                <div className="ptl">
                    <h4 className="text-center mtl">No jobs have been configured.</h4>
                </div>
            )
        }

        return (
            <div>
                <div className="ptm">
                    {
                        this.state.jobs
                            .filter(job => (this.state.filter.length === 0 ? true : this.state.filter.includes(job.repository))
                                && this.state.jobType === job.type)
                            .sort((j1, j2) => j1.repository.localeCompare(j2.repository))
                            .map((job, index) =>
                                this.createJobRow(job, index)
                            )
                    }
                </div>
            </div>
        );
    }
}

export default withAppConfig(JobsView);
