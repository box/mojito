import React from "react";
import {Table} from "react-bootstrap";
import {IntlMixin} from 'react-intl';

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
        state.isSplitToBeRemoved = false;

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
            <RepositoryRow key={repoId} rowData={rowData} onClickProgressBar={this.onClickProgressBar} isBlurred={isBlurred} ref={"repositoryRow" + repoId} />
        );
    },

    onClickProgressBar(repoId) {
        if (this.state.activeRepoId) {
            this.refs["repositoryRow" + this.state.activeRepoId].setInActive();
        }

        this.setState({
            "isLocaleStatsShown": true,
            "activeRepoId": repoId,
            "isSplitToBeRemoved": false
        });
    },

    onCloseRepoLocaleStats() {
        if (this.state.activeRepoId) {
            this.refs["repositoryRow" + this.state.activeRepoId].setInActive();
        }

        this.setState({
            "isLocaleStatsShown": false,
            "activeRepoId": null
        });
    },

    onCloseBeginRepoLocaleStats() {
        this.setState({
            "isSplitToBeRemoved": true
        });
    },

    getRepoLocaleStats() {
        let result = "";
        if (this.state.isLocaleStatsShown) {
            result = (
                <RepositoryStatistic className="split-right" repoId={this.state.activeRepoId} onCloseBegin={this.onCloseBeginRepoLocaleStats} onClose={this.onCloseRepoLocaleStats}/>
            );
        }

        return result;
    },

    render: function () {
        let rows = this.state.repositories.map(this.getTableRow);

        let tableClass = "repo-table";
        if (this.state.isLocaleStatsShown && !this.state.isSplitToBeRemoved) {
            tableClass += " split-left";
        }

        return (
            <div>
                <div>{this.getRepoLocaleStats()}</div>
                <Table className={tableClass}>
                    <thead>
                        <tr>
                            <RepositoryHeaderColumn columnNameMessageId="repositories.table.header.name" />
                            <RepositoryHeaderColumn className="col-md-3" columnNameMessageId="repositories.table.header.status" />
                            <RepositoryHeaderColumn className="col-md-5" columnNameMessageId="repositories.table.header.translated" />
                        </tr>
                    </thead>
                    <tbody>
                    {rows}
                    </tbody>
                </Table>
            </div>
        );
    },

    dataChanged: function (state) {
        this.setState(state);
    }

});

export default Repositories;
