import React from "react";
import {Alert, Button, ButtonGroup, ButtonToolbar, DropdownButton, MenuItem, OverlayTrigger, Table, Tooltip} from "react-bootstrap";
import {IntlMixin, FormattedDate} from "react-intl";
import FluxyMixin from "alt/mixins/FluxyMixin";
import PageRequestParams from "../../sdk/PageRequestParams";
import CancelDropConfig from "../../sdk/drop/CancelDropConfig";
import Drop from "../../sdk/drop/Drop";
import DropActions from "../../actions/drop/dropActions";
import DropStore from "../../stores/drop/DropStore";
import ImportDropConfig from "../../sdk/drop/ImportDropConfig";
import NewDropModal from "./NewDropModal";
import RepositoryActions from "../../actions/RepositoryActions";

let Drops = React.createClass({
    mixins: [IntlMixin, FluxyMixin],

    statics: {
        storeListeners: {
            "onDropStoreUpdated": DropStore
        }
    },

    /** @type {Number} */
    delayedRequestTimeout: null,

    getInitialState() {
        return {
            /** @type {Drop[]} */
            "drops": [],
            /** @type {Drops.FILTER} */
            "filter": Drops.FILTER.IN_PROGRESS,
            /** @type {Number} */
            "currentPageNumber": 0,
            /** @type {Boolean} */
            "hasMoreDrops": false,

            /** @type {Boolean} */
            "isAlertShown": false,
            /** @type {String} */
            "alertMessage": null,
            /** @type {Boolean} */
            "isNewDropModalShown": false,

            /** pagination related attributes  */
            /** @type {Number} */
            "pageSize": 10,
        };
    },
    
    getCurrentPageRequestParam(currentPageNumber) {
        return new PageRequestParams(currentPageNumber, this.state.pageSize);
    },
    
    getPreviousPageRequestParam(currentPageNumber) {
        return new PageRequestParams(currentPageNumber - 1, this.state.pageSize);
    },
    
    getNextPageRequestParam(currentPageNumber) {
        return new PageRequestParams(currentPageNumber + 1, this.state.pageSize);
    },
    
    componentDidMount() {
        RepositoryActions.getAllRepositories();
        
        this.fetchDrops(this.state.filter, this.state.currentPageNumber);
    },

    /**
     * Fetch drops from action given the fitler status.
     *
     * @param {Drops.FILTER} filter
     * @param {Number} currentPageNumber
     */
    fetchDrops(filter, currentPageNumber) {
        switch (filter) {
            case Drops.FILTER.IN_PROGRESS:
                DropActions.getAllInProcess(this.getCurrentPageRequestParam(currentPageNumber));
                break;
            case Drops.FILTER.COMPLETED:
                DropActions.getAllImported(this.getCurrentPageRequestParam(currentPageNumber));
                break;
            case Drops.FILTER.ALL:
                DropActions.getAll(this.getCurrentPageRequestParam(currentPageNumber));
                break;
        }
    },

    componentWillUnmount() {

    },

    /**
     * @param {DropStore} dropStore
     */
    onDropStoreUpdated(dropStore) {

        let drops = dropStore.drops;

        for (let drop of drops) {
            if (drop.status == Drop.STATUS_TYPE.SENDING ||
                drop.status == Drop.STATUS_TYPE.IMPORTING) {
                // Because it's sending, we'll probably get a change of status soon, so let's make a new request again.
                this.delayGetNewRequests();
                break;
            }
        }

        this.setState({
            "drops": drops,
            "hasMoreDrops": dropStore.hasMoreDrops
        });
    },

    openNewRequestModal() {
        this.setState({
            "isNewDropModalShown": true
        });
    },

    /**
     * Closes the new drop modal
     */
    closeNewRequestModal() {
        this.setState({
            "isNewDropModalShown": false
        });

        this.delayGetNewRequests(500);
    },

    /**
     * @param {Number} delay in ms
     * Get all drops after a set timeout
     */
    delayGetNewRequests(delay = 5000) {
        if (this.delayedRequestTimeout) {
            clearTimeout(this.delayedRequestTimeout);
        }

        this.delayedRequestTimeout = setTimeout(() => {
            this.fetchDrops(this.state.filter, this.state.currentPageNumber);
        }, delay);
    },

    /**
     * Handle new request onclick
     */
    onClickNewRequest() {
        this.openNewRequestModal();
    },

    /**
     * Hande import onclick event
     */
    onClickImport(dropId, repoId) {
        this.showAlert(this.getIntlMessage("drops.beingImported.alert"));
        let importDropConfig = new ImportDropConfig(repoId, dropId, null);
        DropActions.importRequest(importDropConfig);

        this.delayGetNewRequests(500);
    },

    /**
     * handle cancel Drop onclick event
     * @param {Number} dropId
     * @param {Number} repoId
     */
    onClickCancel(dropId, repoId) {
        this.showAlert(this.getIntlMessage("drops.beingCanceled.alert"));
        let cancelDropConfig = new CancelDropConfig(dropId, null);
        DropActions.cancelRequest(cancelDropConfig);

        this.delayGetNewRequests(500);
    },

    /**
     * Convert rowData to markup
     *
     * @param {Drop} drop
     * @return {XML}
     */
    getTableRow(drop) {

        let status = "";

        switch (drop.status) {
            case Drop.STATUS_TYPE.IMPORTED:
                status = this.getIntlMessage("drops.status.imported");
                break;
            case Drop.STATUS_TYPE.IMPORTING:
                status = this.getIntlMessage("drops.status.importing");
                break;
            case Drop.STATUS_TYPE.IN_TRANSLATION:
                status = this.getIntlMessage("drops.status.inTranslation");
                break;
            case Drop.STATUS_TYPE.IN_REVIEW:
                status = this.getIntlMessage("drops.status.inReview");
                break;
            case Drop.STATUS_TYPE.SENDING:
                status = this.getIntlMessage("drops.status.sending");
                break;
            case Drop.STATUS_TYPE.CANCELED:
                status = this.getIntlMessage("drops.status.canceled");
                break;
        }

        let wordCount = (drop.translationKits.length > 0) ? drop.translationKits[0].wordCount : "";

        return (
            <tr className="">
                <td>{drop.name}{this.getButtonControlBar(drop)}</td>
                <td>{drop.repository.name}</td>
                <td>{wordCount}</td>
                <td><FormattedDate value={drop.createdDate} day="numeric" month="long" year="numeric"/></td>
                <td>{drop.createdByUser.getDisplayName()}</td>
                <td>{status}</td>
            </tr>
        );
    },

    /**
     * Get control button toolbar
     * @param {Drop} drop
     * @return {JSX}
     */
    getButtonControlBar(drop) {
        let dropId = drop.id;
        let repoId = drop.repository.id;

        let importTitle = <Tooltip>{this.getIntlMessage("drops.controlbar.button.tooltip.import")}</Tooltip>;
        let cancelTitle = <Tooltip>{this.getIntlMessage("drops.controlbar.button.tooltip.cancel")}</Tooltip>;

        let cancelOverlay = "";
        if (!drop.isBeingExported() && !drop.isBeingImported()) {
            cancelOverlay = (<OverlayTrigger placement="top" overlay={cancelTitle}>
                <Button bsStyle="default" onClick={this.onClickCancel.bind(this, dropId, repoId)}>
                    <span className="glyphicon glyphicon-remove" aria-label={cancelTitle}/>
                </Button>
            </OverlayTrigger>);
        }

        return (
            <ButtonToolbar>
                <ButtonGroup>
                    <OverlayTrigger placement="top" overlay={importTitle}>
                        <Button bsStyle="default" onClick={this.onClickImport.bind(this, dropId, repoId)}>
                            <span className="glyphicon glyphicon-import" aria-label={importTitle}/>
                        </Button>
                    </OverlayTrigger>
                    {cancelOverlay}
                </ButtonGroup>
            </ButtonToolbar>
        );
    },

    /**
     * Table Header
     * @return {JSX}
     */
    getTableHeader() {
        return (
            <thead>
            <tr>
                <th>{this.getIntlMessage("drops.tableHeader.name")}</th>
                <th className="col-md-2">{this.getIntlMessage("drops.tableHeader.repository")}</th>
                <th className="col-md-1">{this.getIntlMessage("drops.tableHeader.wordCount")}</th>
                <th className="col-md-2">{this.getIntlMessage("drops.tableHeader.createdDate")}</th>
                <th className="col-md-2">{this.getIntlMessage("drops.tableHeader.createdBy")}</th>
                <th className="col-md-2">{this.getIntlMessage("drops.tableHeader.status")}</th>
            </tr>
            </thead>
        );
    },

    getFilterTitle() {
        let result;

        switch (this.state.filter) {
            case Drops.FILTER.IN_PROGRESS:
                result = this.getIntlMessage("drops.inProgress");
                break;
            case Drops.FILTER.COMPLETED:
                result = this.getIntlMessage("drops.completed");
                break;
            case Drops.FILTER.ALL:
                result = this.getIntlMessage("drops.all");
                break;
            default:
                throw new Error("Unknown filter option");
        }

        return result;
    },

    /**
     *
     * @param {Drops.FILTER} eventKey
     * @param {Object} event
     */
    filterDropDownOnSelect(eventKey, event) {
        this.setState({
            "filter": eventKey,
            "currentPageNumber": 0
        });

        this.fetchDrops(eventKey, 0);
    },

    getDropTable() {
        let tableClass = "drop-table";
        let rows = this.state.drops
            .filter(drop => {
                let toKeep = true;

                if (this.state.filter !== Drops.FILTER.ALL) {
                    toKeep = drop.status !== Drop.STATUS_TYPE.CANCELED;
                }

                return toKeep;
            })
            .map(this.getTableRow);

        return (
            <div>
                <div className="drop-title-bar">
                    {this.getAlert()}
                    {this.getNewRequestButton()}
                    {this.showPagination()}
                    <DropdownButton title={this.getFilterTitle()} className="pull-right mrm mlm"  bsSize="small" onSelect={this.filterDropDownOnSelect}>
                        <MenuItem eventKey={Drops.FILTER.IN_PROGRESS}>{this.getIntlMessage("drops.inProgress")}</MenuItem>
                        <MenuItem eventKey={Drops.FILTER.COMPLETED}>{this.getIntlMessage("drops.completed")}</MenuItem>
                        <MenuItem eventKey={Drops.FILTER.ALL}>{this.getIntlMessage("drops.all")}</MenuItem>
                    </DropdownButton>
                </div>
                <Table className={tableClass}>
                    {this.getTableHeader()}
                    <tbody>
                    {rows}
                    </tbody>
                </Table>
            </div>
        );
    },
    
    /**
     * @return {JSX}
     */
    getNewRequestButton() {
        return (
            <div className="pull-right mrm mlm">
                <Button bsStyle="primary" bsSize="small" className="new-request-button"
                        onClick={this.onClickNewRequest}>
                    {this.getIntlMessage("drops.newRequest.btn")}
                </Button>
            </div>
        );
    },

    /**
     * Handler for when new translation request is made
     */
    onTranslationRequest() {
        this.showAlert(this.getIntlMessage("drops.newTranslationRequest.alert"));
    },

    /**
     * Handler for when new review request is made
     */
    onReviewRequest() {
        this.showAlert(this.getIntlMessage("drops.newReviewRequest.alert"));
    },

    /**
     * @return {XML}
     */
    getNewRequestModal() {
        return (<NewDropModal
                show={this.state.isNewDropModalShown}
                onClose={this.closeNewRequestModal}
                onTranslationRequest={this.onTranslationRequest}
                onReviewRequest={this.onReviewRequest}/>
        );
    },

    /**
     * Handle alert being dismissed
     */
    alertOnDismiss() {
        this.setState({
            "isAlertShown": false
        });
    },

    /**
     * @param {String} message
     * @param message
     */
    showAlert(message) {
        this.setState({
            "isAlertShown": true,
            "alertMessage": message
        });
    },

    /**
     * @return {JSX}
     */
    getAlert() {
        let alert = null;

        if (this.state.isAlertShown && this.state.alertMessage) {
            alert = (
                <div className="drop-alert text-center">
                    <Alert bsStyle="warning" onDismiss={this.alertOnDismiss} dismissAfter={5000}>
                        <span>{this.state.alertMessage}</span>
                    </Alert>
                </div>
            );
        }

        return alert;
    },

    onFetchPreviousPageClicked() {
        
        if (this.state.currentPageNumber > 0) {
            DropActions.getAllInProcess(this.getPreviousPageRequestParam(this.state.currentPageNumber));
        }      
    },
    
    onFetchNextPageClicked() {

        if (this.state.hasMoreDrops) {
            DropActions.getAllInProcess(this.getNextPageRequestParam(this.state.currentPageNumber));
        }
    },
    
    showPagination() {
        let previousPageButtonDisabled = this.state.currentPageNumber == 0;
        let nextPageButtonDisabled = !this.state.hasMoreDrops;
        let currentPageNumber = this.state.currentPageNumber;
        return (
            <div className="drop-pagination pull-right mrm mlm">
                <Button bsSize="small" disabled={previousPageButtonDisabled} onClick={this.onFetchPreviousPageClicked}>
                    <span className="glyphicon glyphicon-chevron-left"></span>
                </Button>
                <label className="mls mrs default-label current-pageNumber">
                    {currentPageNumber + 1}
                </label>
                <Button bsSize="small" disabled={nextPageButtonDisabled} onClick={this.onFetchNextPageClicked}>
                    <span className="glyphicon glyphicon-chevron-right"></span>
                </Button>
            </div>
        );
    },
    
    render() {
        return (
            <div>
                {this.getDropTable()}
                {this.getNewRequestModal()}
            </div>
        );
    }

});

/** @typedef {Drops.FILTER} Pseudo filter for UI display purpose */
Drops.FILTER = {
    "IN_PROGRESS": Symbol(),
    "COMPLETED": Symbol(),
    "ALL": Symbol()
};

export default Drops;
