import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import FluxyMixin from "alt/mixins/FluxyMixin";
import {Button, Modal, OverlayTrigger, Tooltip} from "react-bootstrap";
import BoxSDKConfigActions from "../../actions/boxsdk/BoxSDKConfigActions";
import BoxSDKConfigStore from "../../stores/boxsdk/BoxSDKConfigStore";
import BoxSDKConfig from "../../sdk/entity/BoxSDKConfig";

let BoxSettings = React.createClass({
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

            // Component related states
            "displayMode": this.DISPLAY_MODE.WAITING_FOR_INFO,
            "showWaitModal": false,
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

        if (typeof config === "undefined") {
            this.setState({"displayMode": this.DISPLAY_MODE.WAITING_FOR_INFO});
        } else if (config) {
            let notAvailableMessage = this.props.intl.formatMessage({id: "settings.message.notAvailableYet"});

            this.setState({
                "displayMode": this.DISPLAY_MODE.INFO,
                "showWaitModal": false,
                "clientId": config.clientId,
                "publicKeyId": config.publicKeyId,
                "enterpriseId": config.enterpriseId,
                "appUserId": config.appUserId ? config.appUserId : notAvailableMessage,
                "rootFolderId": config.rootFolderId ? config.rootFolderId : notAvailableMessage,
                "dropsFolderId": config.dropsFolderId ? config.dropsFolderId : notAvailableMessage
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
                       onChange={this.onHandleInputChange}
                />
            </div>
        </div>);
    },

    /**
     * Get a 2 column view of the label message and info
     * @param intlMessageKey
     * @param infoValue
     * @return {XML}
     */
    getLabelAndInfo(intlMessageKey, infoValue) {
        return (<div className="row pbs pts">
            <div className="col-sm-1"></div>
            <label className="col-sm-2 control-label"><FormattedMessage id={intlMessageKey}/></label>
            <div className="col-sm-8">
                <span>{infoValue}</span>
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
     * @param {boolean} shouldBootStrap
     */
    onClickSubmit(shouldBootStrap) {
        let boxSDKConfig = new BoxSDKConfig();
        boxSDKConfig.clientId = this.state.clientId;
        boxSDKConfig.clientSecret = this.state.clientSecret;
        boxSDKConfig.publicKeyId = this.state.publicKeyId;
        boxSDKConfig.privateKey = this.state.privateKey;
        boxSDKConfig.privateKeyPassword = this.state.privateKeyPassword;
        boxSDKConfig.enterpriseId = this.state.enterpriseId;

        BoxSDKConfigActions.setConfig(boxSDKConfig);

        this.setState({"showWaitModal": true});
        this.delayGetUpdatedConfig();
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

        let saveNoBootStrapTitle = <Tooltip><FormattedMessage id="settings.button.title.boxSaveChangesNoBootStrap"/></Tooltip>;
        let saveTitle = <Tooltip><FormattedMessage id="settings.button.title.saveWithBootstrap"/></Tooltip>;

        return (
            <form className="form-horizontal">
                {modal}
                {this.getLabelInputTextBox("settings.box.clientId", "clientId")}
                {this.getLabelInputTextBox("settings.box.clientSecret", "clientSecret")}
                {this.getLabelInputTextBox("settings.box.enterpriseId", "enterpriseId")}
                {this.getLabelInputTextBox("settings.box.publicKeyId", "publicKeyId")}
                {this.getLabelInputTextBox("settings.box.privateKeyPassword", "privateKeyPassword")}
                <div className="form-group pbs pts">
                    <label
                        className="col-sm-2 control-label"><FormattedMessage id="settings.box.privateKey"/></label>
                    <div className="col-sm-8">
                    <textarea className="form-control" rows="10" name="privateKey"
                              placeholder={this.props.intl.formatMessage({ id: "settings.box.privateKey" })}
                              onChange={this.onHandleInputChange}
                    />
                    </div>
                </div>
                <div className="form-group pbs pts">
                    <div className="col-sm-2"></div>
                    <div className="col-sm-8">
                        <OverlayTrigger placement="top" overlay={saveNoBootStrapTitle}>
                            <Button className="pull-right" onClick={this.onClickSubmit.bind(this, true)}>
                                <FormattedMessage id="settings.button.boxSaveChangesNoBootStrap"/>
                            </Button>
                        </OverlayTrigger>
                        <OverlayTrigger placement="top" overlay={saveTitle}>
                            <Button bsStyle="primary" className="mrs pull-right"
                                    onClick={this.onClickSubmit.bind(this, false)}>
                                <FormattedMessage id="settings.button.save"/>
                            </Button>
                        </OverlayTrigger>
                    </div>
                </div>
            </form>);
    },

    /**
     * Get markup for Info View
     * @return {XML}
     */
    getInfoView() {
        return (
            <div>
                {this.getLabelAndInfo("settings.box.clientId", this.state.clientId)}
                {this.getLabelAndInfo("settings.box.enterpriseId", this.state.enterpriseId)}
                {this.getLabelAndInfo("settings.box.publicKeyId", this.state.publicKeyId)}
                {this.getLabelAndInfo("settings.box.appUserId", this.state.appUserId)}
                {this.getLabelAndInfo("settings.box.rootFolderId", this.state.rootFolderId)}
                {this.getLabelAndInfo("settings.box.dropsFolderId", this.state.dropsFolderId)}
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
    }

});

export default injectIntl(BoxSettings);
