import React from "react";
import createReactClass from "create-react-class";
import PropTypes from "prop-types";
import { Button, Modal, Form } from "react-bootstrap";
import RepositoryGeneralInput from "./RepositoryGeneralInput";
import RepositoryLocalesInput from "./RepositoryLocalesInput";
import { deserializeAssetIntegrityCheckers, validateAssetIntegrityCheckers, flattenRepositoryLocales } from "../../utils/RepositoryInputHelper";
import LocaleStore from "../../stores/LocaleStore";
import LocaleActions from "../../actions/LocaleActions";

const STEP = {
    GENERAL: 0,
    LOCALES: 1
};

const DEFAULT_INPUT_STATE = {
    name: "",
    description: "",
    sourceLocale: {},
    checkSLA: false,
    assetIntegrityCheckers: "",
    repositoryLocales: [],
    currentStep: STEP.GENERAL,
}

const RepositoryInputModal  = createReactClass({
    displayName: "RepositoryInputModal",
    propTypes: {
        title: PropTypes.string.isRequired,
        show: PropTypes.bool.isRequired,
        onClose: PropTypes.func.isRequired,
        onSubmit: PropTypes.func.isRequired,
        errorMessage: PropTypes.string,
        isSubmitting: PropTypes.bool
    },

    getInitialState() {
        return { ...DEFAULT_INPUT_STATE, locales: [] };
    },

    componentDidMount() {
        LocaleStore.listen(this.localeStoreChange);
        this.localeStoreChange(LocaleStore.getState());

        if (!LocaleStore.getState().locales || LocaleStore.getState().locales.length === 0) {
            LocaleActions.getLocales();
        }
    },

    componentWillUnmount() {
        LocaleStore.unlisten(this.localeStoreChange);
    },

    localeStoreChange(state) {
        const locales = state.locales || [];
        this.setState({ locales });
    },

    componentDidUpdate(prevProps) {
        if (
            this.props.errorMessage &&
            this.props.errorMessage !== prevProps.errorMessage &&
            this.state.currentStep !== STEP.GENERAL
        ) {
            this.setState({ currentStep: STEP.GENERAL });
        }
        if (!this.props.show && prevProps.show) {
            this.setState({ ...DEFAULT_INPUT_STATE });
        }
    },

    isStepValid(step) {
        if (step === STEP.GENERAL) {
            return this.state.name && this.state.sourceLocale && validateAssetIntegrityCheckers(this.state.assetIntegrityCheckers);
        }
        if (step === STEP.LOCALES) {
            return this.state.repositoryLocales.length > 0;
        }
        return true;
        
    },

    handleCloseModal() {
        this.props.onClose();
    },
    
    handleNextStep() {
        this.setState((prevState) => ({ currentStep: prevState.currentStep + 1 }));
    },

    handlePrevStep() {
        this.setState((prevState) => ({ currentStep: prevState.currentStep - 1 }));
    },

    handleSourceLocaleChange(sourceLocale) {
        this.setState({ sourceLocale });
    },

    handleCheckSLAChange(e) {
        this.setState({ checkSLA: e.target.checked });
    },

    handleRepositoryLocalesChange(repositoryLocales) {
        this.setState({ repositoryLocales });
    },

    handleTextInputChange(e) {
        this.setState({ [e.target.name]: e.target.value });
    },

    handleSubmit(e) {
        e.preventDefault();
        this.props.onSubmit({
            name: this.state.name,
            description: this.state.description,
            sourceLocale: this.state.sourceLocale,
            checkSLA: this.state.checkSLA,
            assetIntegrityCheckers: deserializeAssetIntegrityCheckers(this.state.assetIntegrityCheckers),
            repositoryLocales: flattenRepositoryLocales(this.state.repositoryLocales)
        });
    },

    renderStepContent() {
        switch (this.state.currentStep) {
            case STEP.GENERAL:
                return (
                    <RepositoryGeneralInput
                        name={this.state.name}
                        description={this.state.description}
                        sourceLocale={this.state.sourceLocale}
                        checkSLA={this.state.checkSLA}
                        assetIntegrityCheckers={this.state.assetIntegrityCheckers}
                        hasValidIntegrityCheckers={validateAssetIntegrityCheckers(this.state.assetIntegrityCheckers)}
                        onTextInputChange={this.handleTextInputChange}
                        locales={this.state.locales}
                        onSourceLocaleChange={this.handleSourceLocaleChange}
                        onCheckSLAChange={this.handleCheckSLAChange}
                    />
                );
            case STEP.LOCALES:
                return (
                    <RepositoryLocalesInput
                        locales={this.state.locales}
                        repositoryLocales={this.state.repositoryLocales}
                        onRepositoryLocalesChange={this.handleRepositoryLocalesChange}
                    />
                );
            default:
                return null;
        }
    },

    render() {
        const { currentStep } = this.state;
        const isLastStep = currentStep === STEP.LOCALES;
        const isFirstStep = currentStep === STEP.GENERAL;
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

export default RepositoryInputModal;