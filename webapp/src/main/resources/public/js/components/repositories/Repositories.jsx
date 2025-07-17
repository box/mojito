import React from "react";
import createReactClass from 'create-react-class';
import {Button, Table} from "react-bootstrap";
import ReactSidebarResponsive from "../misc/ReactSidebarResponsive";
import RepositoryStore from "../../stores/RepositoryStore";
import RepositoryHeaderColumn from "./RepositoryHeaderColumn";
import RepositoryRow from "./RepositoryRow";
import RepositoryStatistic from "./RepositoryStatistic";
import FluxyMixin from "alt-mixins/FluxyMixin";
import RepositoryInputModal from "./RepositoryInputModal";

let Repositories = createReactClass({
    displayName: 'Repositories',
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onRepositoryStoreChanged": RepositoryStore
        }
    },

    getInitialState() {
        return {
            repositories: RepositoryStore.getState().repositories,
            isLocaleStatsShown: false,
            activeRepoId: null,
            isRepositoryInputModalOpen: false,
            errorMessage: null,
            isSubmitting: false,
        };
    },

    onRepositoryStoreChanged(state) {
        this.setState({ repositories: state.repositories });

        if (this.state.isRepositoryInputModalOpen && this.state.isSubmitting) {
            if (state.error && state.error.response) {
                state.error.response.text().then(data => {
                    this.setState({ isSubmitting: false, errorMessage: data });
                });
            } else {
                this.setState({
                    isRepositoryInputModalOpen: false,
                    isSubmitting: false,
                    errorMessage: null
                });
            }
        }
    },

    getTableRow(rowData) {
        let repoId = rowData.id;
        let isBlurred = this.state.isLocaleStatsShown && (this.state.activeRepoId !== repoId);

        return (
            <RepositoryRow key={repoId} rowData={rowData} onLocalesButtonToggle={this.onLocalesButtonToggle}
                           isBlurred={isBlurred} ref={"repositoryRow" + repoId}/>
        );
    },

    onLocalesButtonToggle(repoId) {
        if (this.state.isLocaleStatsShown && repoId === this.state.activeRepoId) {
            this.onCloseRequestedRepoLocaleStats();
        } else {
            this.onShowRequestedRepoLocaleStats(repoId);
        }
    },

    onShowRequestedRepoLocaleStats(repoId) {
        this.setState({
            "isLocaleStatsShown": true,
            "activeRepoId": repoId
        });
    },

    onCloseRequestedRepoLocaleStats() {
        this.setState({
            "isLocaleStatsShown": false,
            "activeRepoId": null
        });
    },

    openCreateRepositoryModal() {
        this.setState({ 
            isRepositoryInputModalOpen: true,
            isSubmitting: false,
            errorMessage: null
        });
    },

    closeCreateRepositoryModal() {
        this.setState({ 
            isRepositoryInputModalOpen: false,
            isSubmitting: false,
            errorMessage: null
        });
    },

    handleCreateRepositorySubmit(repository) {
        this.setState({ isSubmitting: true, errorMessage: null });
        RepositoryStore.createRepository(repository);
    },

    render: function () {
        let sideBarContent = "";
        if (this.state.activeRepoId) {
            sideBarContent = <RepositoryStatistic repoId={this.state.activeRepoId}
                                                  onCloseRequest={this.onCloseRequestedRepoLocaleStats}/>;
        }

        return (
            <div>
                <ReactSidebarResponsive ref="sideBar" sidebar={sideBarContent}
                         rootClassName="side-bar-root-container"
                         sidebarClassName="side-bar-container"
                         contentClassName="side-bar-main-content-container"
                         docked={this.state.isLocaleStatsShown} pullRight={true}>
                    <div className="plx prx">
                        <div className="pull-right">
                            <Button bsStyle="primary" onClick={this.openCreateRepositoryModal}>
                                New Repository
                            </Button>
                        </div>
                        <Table className="repo-table table-padded-sides">
                            <thead>
                            <tr>
                                <RepositoryHeaderColumn className="col-md-3"
                                                        columnNameMessageId="repositories.table.header.name"/>
                                <RepositoryHeaderColumn className="col-md-2"/>
                                <RepositoryHeaderColumn className="col-md-3"
                                                        columnNameMessageId="repositories.table.header.needsTranslation"/>
                                <RepositoryHeaderColumn className="col-md-3"
                                                        columnNameMessageId="repositories.table.header.needsReview"/>
                                <RepositoryHeaderColumn className="col-md-1"/>
                            </tr>
                            </thead>
                            <tbody>
                            {this.state.repositories.map(this.getTableRow)}
                            </tbody>
                        </Table>
                    </div>
                </ReactSidebarResponsive>
                <RepositoryInputModal
                    title="Create Repository"
                    show={this.state.isRepositoryInputModalOpen}
                    onClose={this.closeCreateRepositoryModal}
                    onSubmit={this.handleCreateRepositorySubmit}
                    isSubmitting={this.state.isSubmitting}
                    errorMessage={this.state.errorMessage}
                />
            </div>
        );
    },
});

export default Repositories;
