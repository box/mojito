import $ from "jquery";
import React from "react";
import {Table, ProgressBar, Button, Label} from "react-bootstrap";
import {History, Link} from "react-router";
import {IntlMixin, FormattedNumber} from 'react-intl';

import Locales from "../../utils/Locales";
import RepositoryStore from "../../stores/RepositoryStore";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchConstants from "../../utils/SearchConstants";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore.js";

let RepositoryStatistics = React.createClass({
    mixins: [IntlMixin],

    getInitialState() {
        return {};
    },

    getLocaleStatistics() {
        let repoId = this.props.repoId;
        let repo = RepositoryStore.getRepositoryById(repoId);

        let repoStat = repo.repositoryStatistic;
        let repositoryLocaleStatistics = repoStat.repositoryLocaleStatistics;

        // compensate for not fully translated
        let result = "";
        result = repositoryLocaleStatistics.map(repoLocaleStat => {
            let bcp47Tag = repoLocaleStat.locale.bcp47Tag;
            let percent = repoStat.usedTextUnitCount == 0 ? 0 : repoLocaleStat.translatedCount / repoStat.usedTextUnitCount * 100;
            return this.getLocaleStatisticRow(bcp47Tag, percent);
        });

        return result;
    },

    /**
     * Update the Workbench search params to load strings that needs to be translated for a given locale
     *
     * @param {number} repoId
     */
    updateSearchParamsForLocale(bcp47Tag) {

        WorkbenchActions.searchParamsChanged({
                  "changedParam": SearchConstants.UPDATE_ALL,
                  "repoIds": [this.props.repoId],
                  "bcp47Tags": [bcp47Tag],
                  "status": SearchParamsStore.STATUS.FOR_TRANSLATION});
    },

    /**
     *
     * @param {string} bcp47Tag
     * @param {number} percent out of 100
     * @return {XML}
     */
    getLocaleStatisticRow(bcp47Tag, percent) {
        let progressLabel = <FormattedNumber value={(percent / 100).toFixed(3)} style="percent" />;

        return (
            <tr>
                <td className="col-md-6">
                    <div>
                        <Link onClick={this.updateSearchParamsForLocale.bind(this, bcp47Tag)} to='/workbench'>{Locales.getDisplayName(bcp47Tag)}</Link>
                    </div>
                </td>
                <td>
                    <div>
                        <ProgressBar now={percent} label={progressLabel} />
                    </div>
                </td>
            </tr>
        );
    },

    /**
     * Handle onClick close button
     */
    onClickClose() {
        this.props.onCloseBegin();

        $(this.refs.statsContainer.getDOMNode())
            .removeClass("slideInRight")
            .addClass("slideOutRight")
            .one("webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend", () => {
                this.props.onClose();
            });
    },

    render: function () {
        let className = "repo-stats-panel animated slideInRight " + this.props.className;

        return (
            <div className={className} ref="statsContainer">
                <div className="title">
                    <h4>{this.getIntlMessage("repositories.localeStats.localeStats")}</h4>
                    <Button className="close" onClick={this.onClickClose}>×</Button>
                </div>
                <div className="repo-stats-table-container">
                    <Table className="repo-stats-table animated fadeInRight">
                        <tbody>
                            {this.getLocaleStatistics()}
                        </tbody>
                    </Table>
                </div>
            </div>

        );
    }

});

export default RepositoryStatistics;
