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

    propTypes: {

        /** @type {function} */
        "onCloseRequest": React.PropTypes.func.isRequired
    },

    getLocaleStatistics() {
        let repoId = this.props.repoId;
        let repo = RepositoryStore.getRepositoryById(repoId);

        let repoStat = repo.repositoryStatistic;
        let repositoryLocaleStatistics = repoStat.repositoryLocaleStatistics
                .map(this.getRepositoryLocaleStatistics)
                .sort((a, b) => a.localeDisplayName.localeCompare(b.localeDisplayName));

        let toBeFullyTranslatedBcp47Tags = RepositoryStore.getAllToBeFullyTranslatedBcp47TagsForRepo(repoId);

        let rows = repositoryLocaleStatistics.map(repoLocaleStat => {
            let bcp47Tag = repoLocaleStat.bcp47Tag;
            let localeDisplayName = repoLocaleStat.localeDisplayName;
            let isFullyTranslated = toBeFullyTranslatedBcp47Tags.indexOf(bcp47Tag) !== -1;

            return this.getLocaleStatisticRow(bcp47Tag, isFullyTranslated, repoStat.usedTextUnitCount, repoStat.usedTextUnitWordCount, repoLocaleStat.repositoryLocaleStatistic);
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
                    <span className="repo-counts">{numberOfNeedsReview}&nbsp;</span>
                    ({numberOfWordNeedsReview})
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
        let numberOfNeedsTranslation = usedTextUnitCount - repositoryLocaleStatistic.translatedCount + repositoryLocaleStatistic.translationNeededCount;
        let numberOfWordNeedsTranslation = usedTextUnitWordCount - repositoryLocaleStatistic.translatedWordCount + repositoryLocaleStatistic.translationNeededWordCount;

        if (numberOfNeedsTranslation > 0) {
            ui = (
                <Link onClick={this.updateSearchParamsForNeedsTranslation.bind(this, bcp47Tag)} to='/workbench'>
                    <span className="repo-counts">{numberOfNeedsTranslation}&nbsp;</span>
                    ({numberOfWordNeedsTranslation})
                </Link>);
        }

        return ui;
    },

    /**
     * returns RepositoryLocaleStatistics with locale with the display name
     *
     * @param RepositoryLocaleStatistics
     * @return {{bcp47Tag: String, localeDisplayName: String, repositoryLocaleStatistic: RepositoryLocaleStatistics}}
     */
    getRepositoryLocaleStatistics: function (repositoryLocaleStatistic) {
        return {
            "bcp47Tag": repositoryLocaleStatistic.locale.bcp47Tag,
            "localeDisplayName": Locales.getDisplayName(repositoryLocaleStatistic.locale.bcp47Tag),
            "repositoryLocaleStatistic": repositoryLocaleStatistic
        };
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
            <tr className={rowClassName} key={bcp47Tag}>
                <td>
                    <div>
                        <Link onClick={this.updateSearchParamsForLocale.bind(this, bcp47Tag)}
                              to='/workbench'>{Locales.getDisplayName(bcp47Tag)}</Link>
                    </div>
                </td>
                <td>{this.getNeedsTranslationLabel(bcp47Tag, usedTextUnitCount, usedTextUnitWordCount, repositoryLocaleStatistic)}</td>
                <td>{this.getNeedsReviewLabel(bcp47Tag, repositoryLocaleStatistic)}</td>
            </tr>
        );
    },

    render: function () {
        return (
            <div className="repo-stats-panel" ref="statsContainer">
                <div className="title">
                    <Button className="close"
                            onClick={this.props.onCloseRequest ? this.props.onCloseRequest : null}>×</Button>
                </div>
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
