import React from "react";
import {Table, ProgressBar, Button, Label} from "react-bootstrap";
import {History, Link} from "react-router";
import {FormattedMessage, FormattedNumber} from "react-intl";
import Locales from "../../utils/Locales";
import RepositoryStore from "../../stores/RepositoryStore";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchConstants from "../../utils/SearchConstants";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore.js";

let RepositoryStatistics = React.createClass({

    getInitialState() {
        return {};
    },

    getLocaleStatistics() {
        let repoId = this.props.repoId;
        let repo = RepositoryStore.getRepositoryById(repoId);

        let repoStat = repo.repositoryStatistic;
        let repositoryLocaleStatistics = Locales.sortByDisplayName(repoStat.repositoryLocaleStatistics, repositoryLocaleStatistic => repositoryLocaleStatistic.locale.bcp47Tag);

        let toBeFullyTranslatedBcp47Tags = RepositoryStore.getAllToBeFullyTranslatedBcp47TagsForRepo(repoId);

        let rows = repositoryLocaleStatistics.map(repoLocaleStat => {
            let bcp47Tag = repoLocaleStat.locale.bcp47Tag;
            let isFullyTranslated = toBeFullyTranslatedBcp47Tags.indexOf(bcp47Tag) !== -1;

            return this.getLocaleStatisticRow(
                    bcp47Tag, 
                    isFullyTranslated, 
                    repoLocaleStat);
        });


        return rows;
    },

    /**
     * Update the Workbench search params to load strings that needs to be translated for a given locale
     *
     * @param {string} bcp47Tag
     */
    updateSearchParamsForLocale(bcp47Tag) {

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": [this.props.repoId],
            "bcp47Tags": [bcp47Tag],
            "status": SearchParamsStore.STATUS.ALL
        });
    },

    /**
     * Update the Workbench search params to load the for translation view for the selected repo
     *
     * @param {string} bcp47Tag
     */
    updateSearchParamsForNeedsTranslation(bcp47Tag) {

        let repoId = this.props.repoId;

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": [repoId],
            "bcp47Tags": [bcp47Tag],
            "status": SearchParamsStore.STATUS.FOR_TRANSLATION
        });
    },

    /**
     * Update the Workbench search params to load the neeeds review view for the selected repo
     *
     * @param {string} bcp47Tag
     */
    updateSearchParamsForNeedsReview(bcp47Tag) {

        let repoId = this.props.repoId;

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": [repoId],
            "bcp47Tags": [bcp47Tag],
            "status": SearchParamsStore.STATUS.REVIEW_NEEDED
        });
    },


    /**
     * @param {string} bcp47Tag
     * @param {RepositoryLocaleStatistic}
     * @return {XML}
     */
    getNeedsReviewLabel(bcp47Tag, repositoryLocaleStatistic) {

        let ui = "";

        let numberOfNeedsReview = repositoryLocaleStatistic.reviewNeededCount;
        let numberOfWordNeedsReview = repositoryLocaleStatistic.reviewNeededWordCount;

        if (numberOfNeedsReview > 0) {
            ui = (
                <Link onClick={this.updateSearchParamsForNeedsReview.bind(this, bcp47Tag)} to='/workbench'>
                    <span className="repo-counts"><FormattedNumber value={numberOfNeedsReview}/>&nbsp;</span>
                    (&nbsp;<FormattedNumber value={numberOfWordNeedsReview}/>&nbsp;)
                </Link>
            );
        }

        return ui;
    },

    /**
     * @param {string} bcp47Tag
     * @param {number}
     * @param {number}
     * @param {number}
     * @param {RepositoryLocaleStatistic}
     * @return {XML}
     */
    getNeedsTranslationLabel(bcp47Tag, repositoryLocaleStatistic) {

        let ui = "";
       
        if (repositoryLocaleStatistic.forTranslationCount > 0) {
            ui = (
                <Link onClick={this.updateSearchParamsForNeedsTranslation.bind(this, bcp47Tag)} to='/workbench'>
                    <span className="repo-counts"><FormattedNumber value={repositoryLocaleStatistic.forTranslationCount}/>&nbsp;</span>
                    (&nbsp;<FormattedNumber value={repositoryLocaleStatistic.forTranslationWordCount}/>&nbsp;)
                </Link>);
        }

        return ui;
    },

    /**
     * @param {string} bcp47Tag
     * @param {boolean}
     * @param {number}
     * @param {number}
     * @param {number}
     * @param {RepositoryLocaleStatistic}
     * @return {XML}
     */
    getLocaleStatisticRow(
            bcp47Tag, 
            isFullyTranslated, 
            repositoryLocaleStatistic) {

        let rowClassName = "";

        if (!isFullyTranslated) {
            rowClassName = "repo-stats-row-blured";
        }

        return (
            <tr className={rowClassName} key={bcp47Tag}>
                <td>
                    <div>
                        <Link onClick={this.updateSearchParamsForLocale.bind(this, bcp47Tag)}
                              to='/workbench'>{Locales.getDisplayName(bcp47Tag)}</Link>
                    </div>
                </td>
                <td>{this.getNeedsTranslationLabel(
                        bcp47Tag, 
                        repositoryLocaleStatistic)}</td>
                <td>{this.getNeedsReviewLabel(bcp47Tag, repositoryLocaleStatistic)}</td>
            </tr>
        );
    },

    render: function () {
        return (
            <div className="repo-stats-panel" ref="statsContainer">
                <div className="side-bar-content-container prl">
                    <Table className="repo-stats-table">
                        <thead>
                        <tr>
                            <th><FormattedMessage id="drops.tableHeader.locales"/></th>
                            <th><FormattedMessage id='repositories.table.header.needsTranslation'/></th>
                            <th><FormattedMessage id='repositories.table.header.needsReview'/></th>
                        </tr>
                        </thead>
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
