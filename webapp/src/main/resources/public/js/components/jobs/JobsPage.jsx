import React from "react";
import createReactClass from 'create-react-class';
import {withRouter} from "react-router";
import JobTypeDropdown from "./JobTypeDropdown";
import JobsView from "./JobsView";
import AltContainer from "alt-container";
import JobStore from "../../stores/jobs/JobStore";
import RepositoryDropDown from "./RepositoryDropDown";
import { Button } from "react-bootstrap";
import JobActions from "../../actions/jobs/JobActions";
import JobInputModal from "./JobInputModal";

let JobsPage = createReactClass({
    displayName: 'JobsPage',

    getInitialState() {
      return {
          jobType: null,
          showScheduledJobInputModal: false,
          editingJob: null,
          isSubmitting: false,
          errorMessage: null
      };
    },

    componentDidMount() {
        this.jobStoreListener = JobStore.listen(this.jobStoreChange);
    },

    componentWillUnmount() {
        JobStore.unlisten(this.jobStoreListener);
    },

    jobStoreChange(state) {
        if (this.state.showScheduledJobInputModal && this.state.isSubmitting) {
            if (state.error && state.error.response) {
                state.error.response.json().then(data => {
                    this.setState({ isSubmitting: false, errorMessage: data.message });
                })
            } else {
                this.setState({ 
                    showScheduledJobInputModal: false, 
                    editingJob: null, 
                    isSubmitting: false, 
                    errorMessage: null 
                });
            }
        }
    },

    openCreateJobModal() {
        this.setState({
            showScheduledJobInputModal: true,
            editingJob: null,
            errorMessage: null
        });
    },

    closeCreateJobModal() {
        this.setState({
            showScheduledJobInputModal: false, 
            isSubmitting: false, 
            errorMessage: null
        });
    },

    handleCreateJobSubmit(job) {
        this.setState({ isSubmitting: true, errorMessage: null });
        JobActions.createJob(job);
    },

    openEditJobModal(job) {
        this.setState({
            showScheduledJobInputModal: true,
            editingJob: job,
            errorMessage: null
        });
    },

    closeEditJobModal() {
        this.setState({
            showScheduledJobInputModal: false,
            editingJob: null,
            isSubmitting: false,
            errorMessage: null
        });
    },

    handleEditJobSubmit(job) {
        this.setState({ isSubmitting: true, errorMessage: null });
        JobActions.updateJob(job);
    },

    onJobTypeChange(jobType) {
        this.setState({jobType: jobType})
    },

    render: function () {
        const clearLeftFix = {
            clear: 'left',
        };
        return (
            <div>
                <div className="pull-left flex">
                    <JobTypeDropdown onJobTypeChange={this.onJobTypeChange} />
                    <RepositoryDropDown />
                </div>
                <div className="pull-right">
                    <Button bsStyle="primary" onClick={this.openCreateJobModal}>
                        Create Job
                    </Button>
                </div>

                <div style={clearLeftFix}></div>
                
                <JobInputModal
                    title={this.state.editingJob ? "Edit Scheduled Job" : "Create Scheduled Job"}
                    show={this.state.showScheduledJobInputModal}
                    job={this.state.editingJob}
                    onClose={this.state.editingJob ? this.closeEditJobModal : this.closeCreateJobModal}
                    onSubmit={this.state.editingJob ? this.handleEditJobSubmit : this.handleCreateJobSubmit}
                    errorMessage={this.state.errorMessage}
                    isSubmitting={this.state.isSubmitting}
                />

                <AltContainer store={JobStore} className="mtl mbl" >
                    <JobsView jobType={this.state.jobType} openEditJobModal={this.openEditJobModal} />
                </AltContainer>
            </div>
        );
    },
});

export default withRouter(JobsPage);
