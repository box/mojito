import $ from "jquery";
import React from "react";
import {Table} from "react-bootstrap";
import ReactSidebarResponsive from "../misc/ReactSidebarResponsive";
import RepositoryStore from "../../stores/RepositoryStore";
import RepositoryHeaderColumn from "./RepositoryHeaderColumn";
import RepositoryRow from "./RepositoryRow";
import RepositoryActions from "../../actions/RepositoryActions";
import RepositoryStatistic from "../../components/repositories/RepositoryStatistic";
import FluxyMixin from "alt-mixins/FluxyMixin";

let Repositories = React.createClass({

    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onRepositoryStoreChanged": RepositoryStore
        }
    },

    getInitialState: function () {
        let state = RepositoryStore.getState();
        state.isLocaleStatsShown = false;
        state.activeRepoId = null;
        return state;
    },
    
    onRepositoryStoreChanged: function () {
        this.setState(RepositoryStore.getState());
    },

    getTableRow: function (rowData) {
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
            </div>
        );
    }
});

export default Repositories;
