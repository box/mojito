import React from "react";
import createReactClass from 'create-react-class';
import {FormattedMessage, injectIntl} from "react-intl";
import FluxyMixin from "alt-mixins/FluxyMixin";
import {Button, Collapse, Modal, Label, OverlayTrigger, Tooltip} from "react-bootstrap";
import BoxSDKConfigActions from "../../actions/boxsdk/BoxSDKConfigActions";
import BoxSDKConfigStore from "../../stores/boxsdk/BoxSDKConfigStore";
import BoxSDKConfig from "../../sdk/entity/BoxSDKConfig";
import xor from "../../utils/xor";

let BoxSettings = createReactClass({
    displayName: 'BoxSettings',
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onBoxSDKConfigStoreUpdated": BoxSDKConfigStore
        }
    },

    /** @type {Number} */
    delayedRequestTimeout: null,

    DISPLAY_MODE: {
        EDIT: Symbol(),
        INFO: Symbol(),
        WAITING_FOR_INFO: Symbol()
    },

    getInitialState() {
        return {
            // API related state
            "clientId": "",
            "clientSecret": "",
            "publicKeyId": "",
            "privateKey": "",
            "privateKeyPassword": "",
            "enterpriseId": "",
            "appUserId": "",
            "rootFolderId": "",
            "dropsFolderId": "",

            /** @type {boolean} */
            "bootstrap": true,

            /** @type {boolean} */
            "validated": false,

            /** @type {string} */
            "rootFolderUrl": "",

            /** @type {boolean} */
            "isBeingProcessed": false,

            // Component related states
            "displayMode": this.DISPLAY_MODE.WAITING_FOR_INFO,
            "showWaitModal": false,
            "showExtraFields": false
        };
    },

    componentDidMount() {
        BoxSDKConfigActions.getConfig();
    },

    /**
     *
     * @param {BoxSDKConfigStore} boxSDKConfigStore
     */
    onBoxSDKConfigStoreUpdated(boxSDKConfigStore) {
        let config = boxSDKConfigStore.boxSDKConfig;

        this.setState({"isBeingProcessed": boxSDKConfigStore.isBeingProcessed});

        if (typeof config === "undefined") {
            this.setState({"displayMode": this.DISPLAY_MODE.WAITING_FOR_INFO});
        } else if (config) {
            this.setState({
                "displayMode": this.DISPLAY_MODE.INFO,
                "showWaitModal": false,
                "clientId": config.clientId,
                "publicKeyId": config.publicKeyId,
                "enterpriseId": config.enterpriseId,
                "appUserId": config.appUserId,
                "rootFolderId": config.rootFolderId,
                "dropsFolderId": config.dropsFolderId,
                "bootstrap": config.bootstrap,
                "validated": config.validated,
                "rootFolderUrl": config.rootFolderUrl
            });
        } else {
            this.setState({"displayMode": this.DISPLAY_MODE.EDIT});
        }

        if (this.state.showWaitModal) {
            this.delayGetUpdatedConfig();
        }
    },

    /**
     * Gets the Label Input Textbox Markup
     * @param {string} intlMessageKey
     * @param {string} inputName
     * @return {XML}
     */
    getLabelInputTextBox(intlMessageKey, inputName) {
        return (<div className="form-group pbs pts">
            <label className="col-sm-2 control-label"><FormattedMessage id={intlMessageKey}/></label>
            <div className="col-sm-8">
                <input className="form-control" type="text" name={inputName}
                       placeholder={this.props.intl.formatMessage({ id: intlMessageKey })}
                       onChange={this.onHandleInputChange} defaultValue={this.state[inputName]}
                />
            </div>
        </div>);
    },

    /**
     * Get a 2 column view of the label message and info
     * @param intlMessageKey
     * @param infoValue
     * @param optionalMessageIfBlank
     * @return {XML}
     */
    getLabelAndInfo(intlMessageKey, infoValue, optionalMessageIfBlank) {
        return (<div className="row pbs pts">
            <div className="col-sm-1"></div>
            <label className="col-sm-2 control-label"><FormattedMessage id={intlMessageKey}/></label>
            <div className="col-sm-8">
                <span>{!infoValue && optionalMessageIfBlank ? optionalMessageIfBlank : infoValue}</span>
            </div>
        </div>);
    },

    /**
     * Handle when input onChange event.  This updates the component state so that it is up to date with the input value.
     * @param {React.SyntheticEvent} e
     */
    onHandleInputChange(e) {
        let value = e.target.value;
        let newState = {};
        newState[e.target.name] = value;
        this.setState(newState);
    },

    /**
     * Handling submit onclick
     */
    onClickSubmit() {
        let boxSDKConfig = new BoxSDKConfig();
        boxSDKConfig.clientId = this.state.clientId;
        boxSDKConfig.clientSecret = this.state.clientSecret;
        boxSDKConfig.publicKeyId = this.state.publicKeyId;
        boxSDKConfig.privateKey = this.state.privateKey;
        boxSDKConfig.privateKeyPassword = this.state.privateKeyPassword;
        boxSDKConfig.enterpriseId = this.state.enterpriseId;

        if (this.state.appUserId && this.state.dropsFolderId && this.state.rootFolderId) {
            boxSDKConfig.appUserId = this.state.appUserId;
            boxSDKConfig.dropsFolderId = this.state.dropsFolderId;
            boxSDKConfig.rootFolderId = this.state.rootFolderId;
            boxSDKConfig.bootstrap = false;
        }

        BoxSDKConfigActions.setConfig(boxSDKConfig);

        this.setState({"showWaitModal": true});
        this.delayGetUpdatedConfig();
    },

    /**
     * @return {boolean}
     */
    isFormReadyForSubmission() {
        return this.areAllNormalFieldsFilled() && !this.hasDisableBootstrapFieldsError();
    },

    /**
     * @return {boolean}
     */
    areAllNormalFieldsFilled() {
        return (this.state.clientId && this.state.clientSecret && this.state.publicKeyId
        && this.state.privateKey && this.state.privateKeyPassword && this.state.enterpriseId);
    },

    /**
     * @return {boolean}
     */
    hasDisableBootstrapFieldsError() {
        return xor(this.state.appUserId, this.state.dropsFolderId, this.state.rootFolderId);
    },

    /**
     * @return {string|*|string}
     */
    hasAllDisabledBootstrapFields() {
        return this.state.appUserId && this.state.dropsFolderId && this.state.rootFolderId;
    },

    /**
     * Get updated config after a set timeout
     */
    delayGetUpdatedConfig() {
        this.cancelDelayGet();
        this.delayedRequestTimeout = setTimeout(() => {
            BoxSDKConfigActions.getConfig();
        }, 250);
    },

    /**
     * Cancel the currently delayed timeout
     */
    cancelDelayGet() {
        if (this.delayedRequestTimeout) {
            clearTimeout(this.delayedRequestTimeout);
        }
    },

    /**
     * Markup for the edit form
     * @return {XML}
     */
    getForm() {
        let modal = "";
        if (this.state.showWaitModal) {
            modal = (
                <Modal show={true}>
                    <Modal.Header closeButton={false}>
                        <Modal.Title><FormattedMessage id="settings.modal.title.pleaseWait"/></Modal.Title>
                    </Modal.Header>
                    <Modal.Body><FormattedMessage id="settings.modal.box.message"/></Modal.Body>
                </Modal>
            );
        }

        let bootstrapMessage;
        if (this.state.showExtraFields) {
            bootstrapMessage = (
                <OverlayTrigger placement="top"
                                overlay={<Tooltip id="disable-bootstrap-instruction"><FormattedMessage id="settings.label.saveWithoutBootstrapInstructions.tooltip"/></Tooltip>}>
                    <label className="clickable" onClick={() => {this.setState({"showExtraFields": false});}}>
                        <FormattedMessage id="settings.label.saveWithoutBootstrapInstructions"/>
                    </label>
                </OverlayTrigger>);
        } else {
            bootstrapMessage = (
                <OverlayTrigger placement="top"
                                overlay={<Tooltip id="disable-bootstrap-instruction"><FormattedMessage id="settings.label.saveWithoutBootstrapInstructions.tooltip"/></Tooltip>}>
                    <label className="clickable" onClick={() => {this.setState({"showExtraFields": true});}}>
                        <FormattedMessage id="settings.label.saveWithoutBootstrap"/>
                    </label>
                </OverlayTrigger>);
        }

        return (
            <form className="form-horizontal">
                {modal}
                {this.getLabelInputTextBox("settings.box.clientId", "clientId")}
                {this.getLabelInputTextBox("settings.box.clientSecret", "clientSecret")}
                {this.getLabelInputTextBox("settings.box.enterpriseId", "enterpriseId")}
                {this.getLabelInputTextBox("settings.box.publicKeyId", "publicKeyId")}
                {this.getLabelInputTextBox("settings.box.privateKeyPassword", "privateKeyPassword")}
                <div className="form-group pbs pts">
                    <label className="col-sm-2 control-label">
                        <FormattedMessage id="settings.box.privateKey"/>
                    </label>
                    <div className="col-sm-8">
                    <textarea className="form-control" rows="10" name="privateKey"
                              placeholder={this.props.intl.formatMessage({ id: "settings.box.privateKey" })}
                              onChange={this.onHandleInputChange}
                    />
                    </div>
                </div>
                <div className="form-group pbs pts">
                    <div className="col-sm-2"></div>
                    <div className="col-sm-8 text-center">
                        <hr />
                        {bootstrapMessage}
                    </div>
                </div>
                <Collapse in={this.state.showExtraFields}>
                    <div>
                        {this.getLabelInputTextBox("settings.box.appUserId", "appUserId")}
                        {this.getLabelInputTextBox("settings.box.rootFolderId", "rootFolderId")}
                        {this.getLabelInputTextBox("settings.box.dropsFolderId", "dropsFolderId")}
                    </div>
                </Collapse>
                <div className="form-group pbs pts">
                    <div className="col-sm-2"></div>
                    <div className="col-sm-8">
                        {this.state.clientId ?
                            <Button className="pull-right mlm" onClick={() => this.setState({"displayMode": this.DISPLAY_MODE.INFO }) }>
                                <FormattedMessage id="settings.button.cancel"/>
                            </Button> : ""
                        }
                        <Button bsStyle="primary" className="pull-right"
                                disabled={!this.isFormReadyForSubmission()}
                                onClick={this.onClickSubmit}>
                            {this.hasAllDisabledBootstrapFields() ?
                                <FormattedMessage id="settings.button.saveWithoutBootstrapping"/> :
                                <FormattedMessage id="settings.button.saveWithBoostrapping"/>
                            }
                        </Button>
                    </div>
                </div>
            </form>);
    },

    /**
     * Get markup for Info View
     * @return {XML}
     */
    getInfoView() {
        let notAvailableYetMsg = this.props.intl.formatMessage({id: "settings.message.notAvailableYet"});
        
        let validatedResult = (this.state.isBeingProcessed) ? <FormattedMessage id="settings.box.validating" /> :
                (this.state.validated) ?
                    (<Label bsStyle="success"><FormattedMessage id="settings.box.yes"/></Label>) :
                    (<Label bsStyle="danger"><FormattedMessage id="settings.box.no"/></Label>);

        return (
            <div>
                <div className="row pbm ptm">
                    <div className="col-sm-1">
                        <Button bsStyle="primary" className="mlm" onClick={() => this.setState({"displayMode": this.DISPLAY_MODE.EDIT, "showExtraFields": true}) }>
                            <FormattedMessage id="settings.button.edit"/>
                        </Button>
                    </div>
                    <div className="col-sm-2"></div>
                    <div className="col-sm-8"></div>
                </div>
                <div className="row pbs pts">
                    <div className="col-sm-1"></div>
                    <div className="col-sm-2 control-label"><strong><FormattedMessage id="settings.box.validated" /></strong></div>
                    <div className="col-sm-8">{validatedResult}</div>
                </div>
                {this.getLabelAndInfo("settings.box.clientId", this.state.clientId)}
                {this.getLabelAndInfo("settings.box.enterpriseId", this.state.enterpriseId)}
                {this.getLabelAndInfo("settings.box.publicKeyId", this.state.publicKeyId)}
                {this.getLabelAndInfo("settings.box.appUserId", this.state.appUserId, notAvailableYetMsg)}
                {this.getLabelAndInfo("settings.box.rootFolderId", this.state.rootFolderId, notAvailableYetMsg)}
                {this.getLabelAndInfo("settings.box.rootFolderUrl", <a href={this.state.rootFolderUrl}>{this.state.rootFolderUrl}</a>)}
                {this.getLabelAndInfo("settings.box.dropsFolderId", this.state.dropsFolderId, notAvailableYetMsg)}
                <div className="row pbs pts">
                    <div className="col-sm-1"></div>
                    <label className="col-sm-2 control-label"><FormattedMessage id="settings.box.bootstrap" /></label>
                    <div className="col-sm-8">
                        {this.state.bootstrap ? <FormattedMessage id="settings.box.yes" /> : <FormattedMessage id="settings.box.no" />}
                    </div>
                </div>
            </div>
        );
    },

    /**
     *
     * @return {XML}
     */
    getWaitingForInfoView() {
        return (
            <div className="row"><FormattedMessage id="settings.message.waitingForInfo"/></div>
        );
    },

    render() {
        let mainContent;
        if (this.state.displayMode == this.DISPLAY_MODE.EDIT) {
            mainContent = this.getForm();
        } else if (this.state.displayMode == this.DISPLAY_MODE.INFO) {
            mainContent = this.getInfoView();
        } else {
            mainContent = this.getWaitingForInfoView();
        }

        return (
            <div>
                {mainContent}
            </div>
        );
    },
});

export default injectIntl(BoxSettings);
