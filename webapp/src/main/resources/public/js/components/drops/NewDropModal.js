import $ from "jquery";
import React from "react";
import {FormattedMessage} from "react-intl";
import {Button, ListGroupItem, ListGroup, Modal, Table} from "react-bootstrap";
import FluxyMixin from "alt/mixins/FluxyMixin";

import ExportDropConfig from "../../sdk/drop/ExportDropConfig";
import RepositoryActions from "../../actions/RepositoryActions";
import RepositoryStore from "../../stores/RepositoryStore";
import DropActions from "../../actions/drop/dropActions";
import DropStore from "../../stores/drop/DropStore";
import StatusFilter from "../../sdk/entity/StatusFilter";

let NewDropModal = React.createClass({
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {}
    },

    propTypes: {
        "onTranslationRequest": React.PropTypes.func.isRequired,
        "onReviewRequest": React.PropTypes.func.isRequired,
        "onClose": React.PropTypes.func.isRequired
    },

    getInitialState() {
        return {
            "selectedRepoId": null
        };
    },

    componentDidMount() {

    },

    componentWillUnmount() {

    },

    /**
     *
     * @param {SyntheticEvent} e The event object for the click event on text unit action options
     */
    onRepoClick(e) {
        let selectedRepoId = $(e.target).data('repo-id');

        this.setState({
            "selectedRepoId": selectedRepoId
        });
    },

    getRepoListGroup() {
        let repos = RepositoryStore.getState().repositories;

        let reposListGroupItems = repos.map(repo => {
            let isActive = repo.id === this.state.selectedRepoId;
            return <ListGroupItem active={isActive} onClick={this.onRepoClick}
                                  data-repo-id={repo.id}>{repo.name}</ListGroupItem>;
        });

        return (
            <ListGroup>{reposListGroupItems}</ListGroup>
        );
    },

    /**
     * Handling translation button click
     */
    onTranslationClicked() {
        this.props.onTranslationRequest();
        this.onSaveClicked(StatusFilter.Type.Translation);
    },

    /**
     * Handling review button click
     */
    onReviewClicked() {
        this.props.onReviewRequest();
        this.onSaveClicked(StatusFilter.Type.Review);
    },

    /**
     * Make the request and close modal
     * @param {StatusFilter} exportType
     */
    onSaveClicked(exportType) {
        let repoId = this.state.selectedRepoId;

        let exportDropConfig = new ExportDropConfig(repoId, 0, RepositoryStore.getAllToBeFullyTranslatedBcp47TagsForRepo(repoId), null, null);
        exportDropConfig.type = exportType;

        DropActions.createNewRequest(exportDropConfig);

        this.close();
    },

    /**
     * Close the modal
     */
    close() {
        this.props.onClose();
    },

    render() {
        let tableClass = "";
        let isButtonDisabled = true;

        if (this.state.selectedRepoId) {
            isButtonDisabled = false;
        }

        return (
            <Modal show={this.props.show} onHide={this.close}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id="drops.newRequestModal.title" /></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {this.getRepoListGroup()}
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.close}>
                        <FormattedMessage id="label.cancel" />
                    </Button>
                    <Button disabled={isButtonDisabled} onClick={this.onReviewClicked}>
                        <FormattedMessage id="drops.newRequestModal.requestReview" />
                    </Button>
                    <Button bsStyle="primary" disabled={isButtonDisabled} onClick={this.onTranslationClicked}>
                        <FormattedMessage id="drops.newRequestModal.requestTranslation" />
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }

});

export default NewDropModal;
