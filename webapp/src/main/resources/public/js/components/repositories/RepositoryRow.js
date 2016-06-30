import React from "react";
import { ProgressBar, Tooltip, OverlayTrigger, Label, Glyphicon } from "react-bootstrap";
import ReactIntl from "react-intl";
import { History, Link  } from "react-router";

import RepositoryStore from "../../stores/RepositoryStore";
import SearchConstants from "../../utils/SearchConstants";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";

let {IntlMixin, FormattedMessage, FormattedDate, FormattedNumber} = ReactIntl;

let RepositoryRow = React.createClass({

    mixins: [History, IntlMixin],

    /**
     * @return {{percent: number}}
     */
    getInitialState() {
        return {
            "percent": 0,
            "isActive": false,
            "isBlurred": false
        };
    },

    /**
     * Get Repo locale statistics
     * @param {number} repoId
     * @return {object}
     */
    getRepoLocaleStatistics(repoId) {
        let repo = RepositoryStore.getRepositoryById(repoId);
        return repo.repositoryStatistic.repositoryLocaleStatistics;
    },

    /**
     * Get map of repo locale keyed by BCP47 Tag
     * @param {Repository} repo
     * @return {{}}
     */
    getRepoLocalesMapByBcp47Tag(repo) {
        let repoLocalesMap = {};
        let repoLocales = repo.repositoryLocales;
        repoLocales.forEach(repoLocale => {
            repoLocalesMap[repoLocale.locale.bcp47Tag] = {"toBeFullyTranslated": repoLocale.toBeFullyTranslated};
        });

        return repoLocalesMap;
    },

    /**
     * Calculate the average of number of translated strings across all locales in the Repository
     *
     * @param {number} repoId
     * @return {number}
     */
    calculateAvgNumOfTranslated(repoId) {
        let repo = RepositoryStore.getRepositoryById(repoId);
        let repoLocaleStatistics = this.getRepoLocaleStatistics(repoId);
        let repoLocales = repo.repositoryLocales;

        let numFullyTranslated = 0;
        let repoLocalesMap = this.getRepoLocalesMapByBcp47Tag(repo);
        Object.keys(repoLocalesMap).forEach(bcp47Tag => {
            if (repoLocalesMap[bcp47Tag].toBeFullyTranslated) {
                numFullyTranslated++;
            }
        });

        let avgTranslated = 0;
        if (numFullyTranslated > 0) {
            let totalTranslated = 0;
            repoLocaleStatistics.forEach(repoLocaleStat => {
                if (repoLocalesMap[repoLocaleStat.locale.bcp47Tag].toBeFullyTranslated) {
                    totalTranslated += repoLocaleStat.translatedCount;
                }
            });

            avgTranslated = totalTranslated / numFullyTranslated;
        }

        return avgTranslated;
    },

    /**
     * Calculate the average of number of needs review across all locales
     *
     * @param {number} repoId
     * @return {number}
     */
    getNumberOfNeedsReview(repoId) {
        let needsOfNeedsReview = 0;
        let repoLocaleStatistics = this.getRepoLocaleStatistics(repoId);
        repoLocaleStatistics.forEach(repoLocaleStat =>
                needsOfNeedsReview += repoLocaleStat.reviewNeededCount
        );
        return needsOfNeedsReview;
    },

    /**
     * Calculate the total of number of words that needs review across all locales
     *
     * @param {number} repoId
     * @return {number}
     */
    getNumberOfWordNeedsReview(repoId) {
        let needsOfWordNeedsReview = 0;
        let repoLocaleStatistics = this.getRepoLocaleStatistics(repoId);
        repoLocaleStatistics.forEach(repoLocaleStat =>
                needsOfWordNeedsReview += repoLocaleStat.reviewNeededWordCount
        );
        return needsOfWordNeedsReview;
    },

    /**
     * Calculate the total of number of rejected across all locales
     * @param {string} repoId
     * @return {number}
     */
    getNumberOfRejected(repoId) {
        let numberOfRejected = 0;
        let repoLocaleStatistics = this.getRepoLocaleStatistics(repoId);
        repoLocaleStatistics.forEach(repoLocaleStat =>
                numberOfRejected += (repoLocaleStat.translatedCount - repoLocaleStat.includeInFileCount)
        );
        return numberOfRejected;
    },

    /**
     * Calculate the total number of strings for translation (untranslated + needs translation) across all Locales
     *
     * @param {string} repoId
     * @return {number}
     */
    getNumberOfNeedsTranslation(repoId) {
        let numberOfNeedsTranslation = 0;
        let repo = RepositoryStore.getRepositoryById(repoId);
        let repoStats = repo.repositoryStatistic;
        let repoLocalesMap = this.getRepoLocalesMapByBcp47Tag(repo);

        let repoLocaleStatistics = this.getRepoLocaleStatistics(repoId);
        repoLocaleStatistics.forEach(repoLocaleStat => {
            if (repoLocalesMap[repoLocaleStat.locale.bcp47Tag].toBeFullyTranslated) {
                numberOfNeedsTranslation += repoLocaleStat.translationNeededCount;
            }
        });

        return numberOfNeedsTranslation;
    },

    /**
     * Calculate the total number of words for translation (untranslated + needs translation) across all Locales
     *
     * @param {string} repoId
     * @return {number}
     */
    getNumberOfWordNeedsTranslation(repoId) {
        let numberOfWordNeedsTranslation = 0;
        let repo = RepositoryStore.getRepositoryById(repoId);
        let repoStats = repo.repositoryStatistic;
        let repoLocalesMap = this.getRepoLocalesMapByBcp47Tag(repo);

        let repoLocaleStatistics = this.getRepoLocaleStatistics(repoId);
        repoLocaleStatistics.forEach(repoLocaleStat => {
            if (repoLocalesMap[repoLocaleStat.locale.bcp47Tag].toBeFullyTranslated) {
                numberOfWordNeedsTranslation += repoLocaleStat.translationNeededWordCount;
            }
        });

        return numberOfWordNeedsTranslation;
    },

    /**
     * Update the Workbench search params to load the default view for the selected repo
     *
     * @param {number} repoId
     */
    updateSearchParamsForRepoDefault(repoId) {

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": [repoId],
            "bcp47Tags": RepositoryStore.getAllToBeFullyTranslatedBcp47TagsForRepo(repoId)
        });
    },

    /**
     * Update the Workbench search params to load the for translation view for the selected repo
     *
     * @param {number} repoId
     */
    updateSearchParamsForNeedsTranslation(repoId) {

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": [repoId],
            "bcp47Tags": RepositoryStore.getAllToBeFullyTranslatedBcp47TagsForRepo(repoId),
            "status": SearchParamsStore.STATUS.FOR_TRANSLATION
        });
    },

    /**
     * Update the Workbench search params to load the neeeds review view for the selected repo
     *
     * @param {number} repoId
     */
    updateSearchParamsForNeedsReview(repoId) {

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": [repoId],
            "bcp47Tags": RepositoryStore.getAllToBeFullyTranslatedBcp47TagsForRepo(repoId),
            "status": SearchParamsStore.STATUS.REVIEW_NEEDED
        });
    },

    /**
     * Update the Workbench search params to load the rejected view for the selected repo
     *
     * @param {number} repoId
     */
    updateSearchParamsForRejected(repoId) {

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": [repoId],
            "bcp47Tags": RepositoryStore.getAllToBeFullyTranslatedBcp47TagsForRepo(repoId),
            "status": SearchParamsStore.STATUS.REJECTED
        });
    },

    /**
     * @param {number} numberRejected
     * @return {XML}
     */
    getRejectedLabel() {

        let ui = "";

        const repoId = this.props.rowData.id;
        let numberOfRejected = this.getNumberOfRejected(repoId);

        if (numberOfRejected > 0) {
            ui = (
                    <Link onClick={this.updateSearchParamsForRejected.bind(this, repoId)} to='/workbench' className="label-container status-label-container">
                        <Label bsStyle="danger" className="mrs clickable">
                            <FormattedMessage numberOfRejected={numberOfRejected} message={this.getIntlMessage("repositories.table.row.rejected")}/>
                        </Label>
                    </Link>);
        }

        return ui;
    },

    /**
     * @return {XML}
     */
    getNeedsReviewLabel() {

        let ui = "";

        const repoId = this.props.rowData.id;
        let numberOfNeedsReview = this.getNumberOfNeedsReview(repoId);
        let numberOfWordNeedsReview = this.getNumberOfWordNeedsReview(repoId);

        if (numberOfNeedsReview > 0) {
            ui = (
                    <Link onClick={this.updateSearchParamsForNeedsReview.bind(this, repoId)} to='/workbench' className="">
                        <span className="repo-counts">{numberOfNeedsReview} </span>
                        (
                        <FormattedMessage
                                numberOfWords={numberOfWordNeedsReview}
                                message={this.getIntlMessage("repositories.table.row.numberOfWords")}/>
                        )
                    </Link>
            );
        }

        return ui;
    },

    /**
     * @return {XML}
     */
    getNeedsTranslationLabel() {

        let ui = "";

        const repoId = this.props.rowData.id;
        let numberOfNeedsTranslation = this.getNumberOfNeedsTranslation(repoId);
        let numberOfWordNeedsTranslation = this.getNumberOfWordNeedsTranslation(repoId);

        if (numberOfNeedsTranslation > 0) {
            ui = (
                    <Link onClick={this.updateSearchParamsForNeedsTranslation.bind(this, repoId)} to='/workbench'>
                        <span className="repo-counts">{numberOfNeedsTranslation} </span>
                        (
                        <FormattedMessage
                                numberOfWords={numberOfWordNeedsTranslation}
                                message={this.getIntlMessage("repositories.table.row.numberOfWords")}/>
                        )
                    </Link>);
        }

        return ui;
    },

    /**
     * Handle when progress bar is clicked
     */
    onClickShowLocalesButton() {
        if (!this.state.isActive) {
            this.setState({
                "isActive": true
            });

            this.props.onClickShowLocalesButton(this.props.rowData.id);
        }
    },

    /**
     * Set state to be inactive
     */
    setInActive() {
        this.setState({
            "isActive": false
        });
    },

    /**
     * @return {XML}
     */
    render() {
        const repoId = this.props.rowData.id;

        let descriptionTooltip = <div />;

        if (this.props.rowData.description) {
            descriptionTooltip =
                    <Tooltip>{this.props.rowData.description}</Tooltip>;
        }

        let rowClass = "";

        if (this.state.isActive) {
            rowClass = "repo-active";
        } else if (this.props.isBlurred) {
            rowClass = "repo-blurred";
        }

        return (
                <tr className={rowClass}>
                    <td className="repo-name" overlay={descriptionTooltip}>
                        <OverlayTrigger placement="right" overlay={descriptionTooltip}>
                            <Link onClick={this.updateSearchParamsForRepoDefault.bind(this, repoId)} to='/workbench'>{this.props.rowData.name}</Link>
                        </OverlayTrigger>
                    </td>

                    <td>{this.getRejectedLabel()}</td>
                    <td>{this.getNeedsTranslationLabel()}</td>
                    <td>{this.getNeedsReviewLabel()}</td>
                    <td>
                        <Label className="clickable label label-primary repo-show-locales-button" onClick={this.onClickShowLocalesButton}><Glyphicon glyph="option-horizontal"/></Label>
                    </td>
                </tr>
        );
    }
});
export default RepositoryRow;
