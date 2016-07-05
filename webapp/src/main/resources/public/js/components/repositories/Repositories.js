import $ from "jQuery";
import React from "react";
import {Table} from "react-bootstrap";
import {IntlMixin} from "react-intl";
import SideBar from "react-sidebar";
import RepositoryStore from "../../stores/RepositoryStore";
import RepositoryHeaderColumn from "./RepositoryHeaderColumn";
import RepositoryRow from "./RepositoryRow";
import RepositoryActions from "../../actions/RepositoryActions";
import RepositoryStatistic from "../../components/repositories/RepositoryStatistic";

let Repositories = React.createClass({
    mixins: [IntlMixin],

    getInitialState: function () {
        let state = RepositoryStore.getState();

        state.isLocaleStatsShown = false;
        state.activeRepoId = null;

        return state;
    },

    componentDidMount: function () {

        // TODO remove this when upgrading react-sidebar to 2.0 (when upgrading to react 15)
        $(this.refs.sideBar.refs.sidebar.getDOMNode()).parent().addClass("side-bar-container");

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
                <SideBar ref="sideBar" sidebar={sideBarContent}
                         docked={this.state.isLocaleStatsShown} pullRight={true}>
                    <div>
                        <Table className="repo-table">
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
                </SideBar>
            </div>
        );
    },

    dataChanged: function (state) {
        this.setState(state);
    }

});

export default Repositories;
