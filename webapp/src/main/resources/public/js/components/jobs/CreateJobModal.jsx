import React from "react";
import createReactClass from "create-react-class";
import PropTypes from "prop-types";
import { Button, Modal, Form } from "react-bootstrap";
import CreateJobRepositoryDropDown from "./CreateJobRepositoryDropDown";
import JobActions from "../../actions/jobs/JobActions";
import { JobType, JobTypeIds } from "../../utils/JobType";
import JobTypeDropdown from "./JobTypeDropdown";

const CreateJobModal = createReactClass({
    displayName: "CreateJobModal",
    propTypes: {
        show: PropTypes.bool.isRequired,
        onClose: PropTypes.func.isRequired,
    },


    getInitialState() {
        return {
            selectedRepository: null,
            jobType: JobType.THIRD_PARTY_SYNC,
            cron: "0 0 0 * * ?",
            thirdPartyProjectId: "",
            skipTextUnitsWithPattern: "",
            pluralSeparator: " _",
        };
    },

    onHandleInputChange(e) {
        const { name, value } = e.target;
        this.setState({ [name]: value });
    },

    handleRepositorySelect(selectedRepository) {
        this.setState({ selectedRepository });
    },

    handleSubmit() {
        const jobToSave = {
            repository: this.state.selectedRepository,
            propertiesString: JSON.stringify({
                version: 1,
                thirdPartyProjectId: this.state.thirdPartyProjectId,
                actions: ["PUSH", "PULL", "MAP_TEXTUNIT"],
                pluralSeparator: this.state.pluralSeparator,
                localeMapping: "",
                skipTextUnitsWithPattern: this.state.skipTextUnitsWithPattern,
                skipAssetsWithPathPattern: "test_ignore%",
                includeTextUnitsWithPattern: "",
                options: ["smartling-placeholder-format=NONE"]
            }),
            cron: this.state.cron,
            jobType: { id: JobTypeIds[this.state.jobType] },
        }
        JobActions.createJob(jobToSave);
        this.setState({ selectedRepository: null });
        this.props.onClose();
    },

    onJobTypeChange(jobType) {
        this.setState({jobType: jobType})
    },

    getLabelInputTextBox(label, placeholder, inputName) {
        return (
            <div className="form-group pbs pts">
                <label className="col-sm-2 control-label">{label}</label>
                <div className="col-sm-8">
                    <input
                        className="form-control"
                        type="text"
                        name={inputName}
                        placeholder={placeholder}
                        value={this.state[inputName]}
                        onChange={this.onHandleInputChange}
                    />
                </div>
            </div>
        );
    },


    render() {
        return (
            <Modal show={this.props.show} onHide={this.props.onClose}>
                <Form
                    onSubmit={e => {
                        e.preventDefault();
                        this.handleSubmit();
                    }}
                >
                    <Modal.Header closeButton>
                        <Modal.Title>Create a Scheduled Job</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <div className="form-group pbs">
                            <label className="col-sm-2 control-label">Repository</label>
                            <div className="col-sm-8">
                            <CreateJobRepositoryDropDown
                                selected={this.state.selectedRepository}
                                onSelect={this.handleRepositorySelect}
                            />
                            </div>
                        </div>
                        <div className="form-group pbs">
                            <label className="col-sm-2 control-label">Job Type</label>
                            <div>
                                <JobTypeDropdown onJobTypeChange={this.onJobTypeChange} />
                            </div>
                        </div>

                        {this.getLabelInputTextBox("Sync Frequency (Cron)", "Enter cron expression", "cron")}
                        {this.getLabelInputTextBox("Third Party Project ID", "Enter Smartling Project Id", "thirdPartyProjectId")}
                        {this.getLabelInputTextBox("Skip Text Units With Pattern", "Enter skip text units pattern", "skipTextUnitsWithPattern")}
                        {this.getLabelInputTextBox("Plural Separator", "Enter plural separator", "pluralSeparator")}
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={this.props.onClose}>
                            Close
                        </Button>
                        <Button
                            variant="primary"
                            type="submit"
                            disabled={!this.state.selectedRepository}
                        >
                            Submit
                        </Button>
                    </Modal.Footer>
                </Form>
            </Modal>
        );
    }
});

export default CreateJobModal;