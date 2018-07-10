import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage} from "react-intl";
import {Button, ListGroupItem, ListGroup, Modal, Table} from "react-bootstrap";
import FluxyMixin from "alt-mixins/FluxyMixin";
import ExportDropConfig from "../../sdk/drop/ExportDropConfig";
import RepositoryStore from "../../stores/RepositoryStore";
import DropActions from "../../actions/drop/dropActions";
import StatusFilter from "../../sdk/entity/StatusFilter";

let NewDropModal = React.createClass({
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {}
    },

    propTypes: {
        "onTranslationRequest": PropTypes.func.isRequired,
        "onReviewRequest": PropTypes.func.isRequired,
        "onClose": PropTypes.func.isRequired
    },

    getInitialState() {
        return {
            "selectedRepoSet": new Set()
        };
    },

    /**
     *
     * @param {string} repoId
     */
    onRepoClick(repoId) {
        let repoSet = this.state.selectedRepoSet;
        if (repoSet.has(repoId)) {
            repoSet.delete(repoId);
        } else {
            repoSet.add(repoId);
        }
        this.setState({
            "selectedRepoSet": repoSet
        });
    },

    getRepoListGroup() {
        let repos = RepositoryStore.getState().repositories;

        let repoSet = this.state.selectedRepoSet;

        let reposListGroupItems = repos.map(repo => {
            let isActive = repoSet.has(repo.id);
            return (
                <ListGroupItem key={"NewDropModal." + repo.name} active={isActive} onClick={this.onRepoClick.bind(this, repo.id)}>
                    {repo.name}
                </ListGroupItem>
            );
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
        let repoSet = this.state.selectedRepoSet;
        repoSet.forEach((repoId) => {
            let exportDropConfig = new ExportDropConfig(repoId, 0, RepositoryStore.getAllToBeFullyTranslatedBcp47TagsForRepo(repoId), null, null);
            exportDropConfig.type = exportType;

            DropActions.createNewRequest(exportDropConfig);
        });
        this.state.selectedRepoSet.clear();
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

        // enable the button if there are any entries in the set
        const iterator = this.state.selectedRepoSet.entries();
        let n = iterator.next();
        if (n && !n.done) {
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
