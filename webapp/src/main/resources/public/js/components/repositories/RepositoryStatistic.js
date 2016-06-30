import $ from "jquery";
import _ from "lodash";
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

        let toBeFullyTranslatedBcp47Tags = RepositoryStore.getAllToBeFullyTranslatedBcp47TagsForRepo(repoId);

        let rows = repositoryLocaleStatistics.map(repoLocaleStat => {
            let bcp47Tag = repoLocaleStat.locale.bcp47Tag;
            let isFullyTranslated = toBeFullyTranslatedBcp47Tags.indexOf(bcp47Tag) !== -1;

            return this.getLocaleStatisticRow(bcp47Tag, isFullyTranslated, repoStat.usedTextUnitCount, repoStat.usedTextUnitWordCount, repoLocaleStat);
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
                        <span className="repo-counts">{numberOfNeedsReview}</span> ({numberOfWordNeedsReview})
                    </Link>
            );
        }

        return ui;
    },

    /**
     * @param {string} bcp47Tag
     * @param {number}
     * @param {number}
     * @param {RepositoryLocaleStatistic}
     * @return {XML}
     */
    getNeedsTranslationLabel(bcp47Tag, usedTextUnitCount, usedTextUnitWordCount, repositoryLocaleStatistic) {

        let ui = "";

        let numberOfNeedsTranslation = repositoryLocaleStatistic.translationNeededCount;
        let numberOfWordNeedsTranslation = repositoryLocaleStatistic.translationNeededWordCount;

        if (numberOfNeedsTranslation > 0) {
            ui = (
                    <Link onClick={this.updateSearchParamsForNeedsTranslation.bind(this, bcp47Tag)} to='/workbench'>
                        <span className="repo-counts">{numberOfNeedsTranslation}</span> ({numberOfWordNeedsTranslation})
                    </Link>);
        }

        return ui;
    },

    /**
     * @param {string} bcp47Tag
     * @param {boolean}
     * @param {number}
     * @param {number}
     * @param {RepositoryLocaleStatistic}
     * @return {XML}
     */
    getLocaleStatisticRow(bcp47Tag, isFullyTranslated, usedTextUnitCount, usedTextUnitWordCount, repositoryLocaleStatistic) {

        let rowClassName = "";

        if (!isFullyTranslated) {
            rowClassName = "repo-stats-row-blured";
        }

        return (
                <tr className={rowClassName}>
                    <td>
                        <div>
                            <Link onClick={this.updateSearchParamsForLocale.bind(this, bcp47Tag)} to='/workbench'>{Locales.getDisplayName(bcp47Tag)}</Link>
                        </div>
                    </td>
                    <td>{this.getNeedsTranslationLabel(bcp47Tag, usedTextUnitCount, usedTextUnitWordCount, repositoryLocaleStatistic)}</td>
                    <td>{this.getNeedsReviewLabel(bcp47Tag, repositoryLocaleStatistic)}</td>
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
                        <Button className="close" onClick={this.onClickClose}>×</Button>
                    </div>
                    <div className="repo-stats-table-container">
                        <Table className="repo-stats-table animated fadeInRight">
                            <thead>
                            <tr>
                                <th>Locales</th>
                                <th>{this.getIntlMessage('repositories.table.header.needsTranslation')}</th>
                                <th>{this.getIntlMessage('repositories.table.header.needsReview')}</th>
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
