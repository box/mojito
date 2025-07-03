import React from "react";
import createReactClass from "create-react-class";
import PropTypes from "prop-types";
import { Button, Modal, Form } from "react-bootstrap";
import { JobType } from "../../utils/JobType";
import JobGeneralInput from "./JobGeneralInput";
import JobThirdPartyInput from "./JobThirdPartyInput";
import JobAdvancedInput from "./JobAdvancedInput";
import { validateCronExpression } from "../../utils/CronExpressionHelper";
import { parseLocaleMappingString, serializeLocaleMappingArray, parseOptionsArray, serializeOptionsArray } from "../../utils/JobInputHelper";

const DEFAULT_STATE = {
    id: null,
    selectedRepository: null,
    jobType: JobType.THIRD_PARTY_SYNC,
    cron: "",
    thirdPartyProjectId: "",
    selectedActions: ["PUSH", "PULL", "MAP_TEXTUNIT", "PUSH_SCREENSHOT"],
    localeMapping: [],
    skipTextUnitsWithPattern: "",
    pluralSeparator: "",
    skipAssetsWithPathPattern: "",
    includeTextUnitsWithPattern: "",
    options: [],
    currentStep: 0
};

const JobInputModal  = createReactClass({
    displayName: "JobInputModal",
    propTypes: {
        title: PropTypes.string.isRequired,
        show: PropTypes.bool.isRequired,
        onClose: PropTypes.func.isRequired,
        onSubmit: PropTypes.func.isRequired,
        job: PropTypes.object,
        errorMessage: PropTypes.string,
        isSubmitting: PropTypes.bool
    },

    getInitialState() {
        return { ...DEFAULT_STATE };
    },

    componentDidUpdate(prevProps) {
        if (
            this.props.errorMessage &&
            this.props.errorMessage !== prevProps.errorMessage &&
            this.state.currentStep !== 0
        ) {
            this.setState({ currentStep: 0 });
        }
        if (prevProps.job !== this.props.job) {
            if (this.props.job) {
                const jobProperties = this.props.job.properties || {};
                this.setState({
                    id: this.props.job.id,
                    selectedRepository: this.props.job.repository || null,
                    jobType: this.props.job.type || JobType.THIRD_PARTY_SYNC,
                    cron: this.props.job.cron || "",
                    thirdPartyProjectId: jobProperties.thirdPartyProjectId || "",
                    selectedActions: jobProperties.actions || [],
                    localeMapping: parseLocaleMappingString(jobProperties.localeMapping),
                    skipTextUnitsWithPattern: jobProperties.skipTextUnitsWithPattern || "",
                    pluralSeparator: jobProperties.pluralSeparator || "",
                    skipAssetsWithPathPattern: jobProperties.skipAssetsWithPathPattern || "",
                    includeTextUnitsWithPattern: jobProperties.includeTextUnitsWithPattern || "",
                    options: parseOptionsArray(jobProperties.options || [])
                });
            } else {
                this.clearModal();
            }
        }
    },

    getScheduledJobInput() {
        return {
            id: this.state.id,
            repository: this.state.selectedRepository,
            propertiesString: JSON.stringify({
                thirdPartyProjectId: this.state.thirdPartyProjectId,
                actions: this.state.selectedActions,
                pluralSeparator: this.state.pluralSeparator,
                localeMapping: serializeLocaleMappingArray(this.state.localeMapping),
                skipTextUnitsWithPattern: this.state.skipTextUnitsWithPattern,
                skipAssetsWithPathPattern: this.state.skipAssetsWithPathPattern,
                includeTextUnitsWithPattern: this.state.includeTextUnitsWithPattern,
                options: serializeOptionsArray(this.state.options)
            }),
            cron: this.state.cron,
            type: this.state.jobType,
        };
    },

    clearModal() {
        this.setState({ ...DEFAULT_STATE });
    },


    onHandleInputChange(e) {
        const { name, value } = e.target;
        this.setState({ [name]: value });
    },

    handleRepositorySelect(selectedRepository) {
        this.setState({ selectedRepository });
    },

    handleActionsChange(actions) {
        this.setState({ selectedActions: actions });
    },

    handleJobTypeChange(jobType) {
        this.setState({jobType: jobType})
    },

    handleLocaleMappingChange(newMapping) {
        this.setState({ localeMapping: newMapping });
    },

    handleOptionsMappingChange(newOptions) {
        this.setState({ options: newOptions });
    },

    handleSubmit(e) {
        e.preventDefault();
        const scheduledJobInput = this.getScheduledJobInput();
        this.props.onSubmit(scheduledJobInput);
        this.setState({ currentStep: 0 });
    },

    handleCloseModal() {
        this.props.onClose();
        this.clearModal();
    },

    isStepValid(step) {
        if (step === 0) {
            return this.state.selectedRepository &&
                this.state.jobType &&
                this.state.cron &&
                validateCronExpression(this.state.cron);
        }
        if (step === 1) {
            return this.state.thirdPartyProjectId &&
                Array.isArray(this.state.selectedActions) &&
                this.state.selectedActions.length > 0;
        }
        return true;
    },

    renderStepContent() {
        const { currentStep } = this.state;
        switch (currentStep) {
            case 0:
                return (
                    <JobGeneralInput
                        selectedRepository={this.state.selectedRepository}
                        onRepositorySelect={this.handleRepositorySelect}
                        jobType={this.state.jobType}
                        onJobTypeChange={this.handleJobTypeChange}
                        cron={this.state.cron}
                        onInputChange={this.onHandleInputChange}
                        getLabelInputTextBox={this.getLabelInputTextBox}
                    />
                );
            case 1:
                return (
                    <JobThirdPartyInput
                        thirdPartyProjectId={this.state.thirdPartyProjectId}
                        onInputChange={this.onHandleInputChange}
                        selectedActions={this.state.selectedActions}
                        onActionsChange={this.handleActionsChange}
                        localeMapping={this.state.localeMapping}
                        onLocaleMappingChange={this.handleLocaleMappingChange}
                        getLabelInputTextBox={this.getLabelInputTextBox}
                    />
                );
            case 2:
                return (
                    <JobAdvancedInput
                        pluralSeparator={this.state.pluralSeparator}
                        skipTextUnitsWithPattern={this.state.skipTextUnitsWithPattern}
                        skipAssetsWithPathPattern={this.state.skipAssetsWithPathPattern}
                        includeTextUnitsWithPattern={this.state.includeTextUnitsWithPattern}
                        options={this.state.options}
                        onInputChange={this.onHandleInputChange}
                        onOptionsMappingChange={this.handleOptionsMappingChange}
                        getLabelInputTextBox={this.getLabelInputTextBox}
                    />
                );
            default:
                return null;
        }
    },

    handleNextStep() {
        this.setState((prevState) => ({ currentStep: prevState.currentStep + 1 }));
    },

    handlePrevStep() {
        this.setState((prevState) => ({ currentStep: prevState.currentStep - 1 }));
    },

    render() {
        const { currentStep } = this.state;
        const isLastStep = currentStep === 2;
        const isFirstStep = currentStep === 0;
        return (
            <Modal show={this.props.show} onHide={this.handleCloseModal}>
                <Form
                    onSubmit={this.handleSubmit}
                >
                    <Modal.Header closeButton>
                        <Modal.Title>{this.props.title}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        {this.props.errorMessage && (
                            <div className="alert alert-danger">
                                {this.props.errorMessage}
                            </div>
                        )}
                        {this.renderStepContent()}
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={this.handleCloseModal}>
                            Close
                        </Button>
                        {!isFirstStep && (
                            <Button variant="secondary" onClick={this.handlePrevStep}>
                                Back
                            </Button>
                        )}
                        {!isLastStep && (
                            <Button
                                variant="primary"
                                onClick={this.handleNextStep}
                                disabled={!this.isStepValid(currentStep) || this.props.isSubmitting}
                            >
                                Next
                            </Button>
                        )}
                        {isLastStep && (
                            <Button
                                variant="primary"
                                type="submit"
                                disabled={!this.isStepValid(currentStep) || this.props.isSubmitting}
                            >
                                {this.props.isSubmitting ? "Submitting..." : "Submit"}
                            </Button>
                        )}
                    </Modal.Footer>
                </Form>
            </Modal>
        );
    }
});

export default JobInputModal;