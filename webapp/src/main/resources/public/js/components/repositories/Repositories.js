import $ from "jQuery";
import React from "react";
import {Table} from "react-bootstrap";
import ReactSidebarResponsive from "../misc/ReactSidebarResponsive";
import RepositoryStore from "../../stores/RepositoryStore";
import RepositoryHeaderColumn from "./RepositoryHeaderColumn";
import RepositoryRow from "./RepositoryRow";
import RepositoryActions from "../../actions/RepositoryActions";
import RepositoryStatistic from "../../components/repositories/RepositoryStatistic";

let Repositories = React.createClass({

    getInitialState: function () {
        let state = RepositoryStore.getState();

        state.isLocaleStatsShown = false;
        state.activeRepoId = null;

        return state;
    },

    componentDidMount: function () {
        RepositoryActions.init();

        RepositoryStore.listen(this.dataChanged);
    },

    componentWillUnmount: function () {
        RepositoryStore.unlisten(this.dataChanged);
    },

    getTableRow: function (rowData) {
        let repoId = rowData.id;
        let isBlurred = this.state.isLocaleStatsShown && (this.state.activeRepoId !== repoId);

        return (
            <RepositoryRow key={repoId} rowData={rowData} onClickShowLocalesButton={this.onClickShowLocalesButton}
                           isBlurred={isBlurred} ref={"repositoryRow" + repoId}/>
        );
    },

    onClickShowLocalesButton(repoId) {
        if (this.state.activeRepoId) {
            this.refs["repositoryRow" + this.state.activeRepoId].setInActive();
        }

        this.setState({
            "isLocaleStatsShown": true,
            "activeRepoId": repoId
        });
    },

    onCloseRequestedRepoLocaleStats() {
        if (this.state.activeRepoId) {
            this.refs["repositoryRow" + this.state.activeRepoId].setInActive();
        }

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
    },

    dataChanged: function (state) {
        this.setState(state);
    }

});

export default Repositories;
