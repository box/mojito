import React from "react";
import { ProgressBar, Tooltip, OverlayTrigger, Label } from "react-bootstrap";
import ReactIntl from "react-intl";
import { History, Link  } from "react-router";

import RepositoryStore from "../../stores/RepositoryStore";
import SearchConstants from "../../utils/SearchConstants";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore"

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
     * Invoked once, only on the client (not on the server), immediately after the initial rendering occurs.
     * At this point in the lifecycle, the component has a DOM representation which you can access via
     * React.findDOMNode(this). The componentDidMount() method of child components is invoked before that of parent components.
     *
     * If you want to integrate with other JavaScript frameworks, set timers using setTimeout or setInterval, or
     * send AJAX requests, perform those operations in this method.
     */
    componentDidMount() {
        let percentTranslated = this.getPercentTranslated();

        setTimeout(() => {
            this.setState({
                "percent": percentTranslated * 100
            });
        }, 10);
    },

    /**
     * Get percentage of translated text unit across all locales
     * @return {number}
     */
    getPercentTranslated() {
        let percentTranslated = 0;

        let repoId = this.props.rowData.id;
        let repo = RepositoryStore.getRepositoryById(repoId);

        if (repo.repositoryStatistic.usedTextUnitCount > 0) {
            let avgTranslated = this.calculateAvgNumOfTranslated(repoId);
            percentTranslated = avgTranslated / repo.repositoryStatistic.usedTextUnitCount;
        }

        return percentTranslated;
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
     * Calculate the average of number of rejected across all locales
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
                numberOfNeedsTranslation += (repoStats.usedTextUnitCount - repoLocaleStat.translatedCount + repoLocaleStat.translationNeededCount);
            }
        });

        return numberOfNeedsTranslation;
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
            "status":  SearchParamsStore.STATUS.REVIEW_NEEDED
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
            "status":  SearchParamsStore.STATUS.REJECTED
        });
    },

    /**
     * @param {number} numberForReview
     * @return {XML}
     */
    getNeedsReviewLabel(numberOfNeedsReview) {
        const repoId = this.props.rowData.id;
        return (
            <Link onClick={this.updateSearchParamsForNeedsReview.bind(this, repoId)} to='/workbench' className="label-container">
                <Label bsStyle="info" className="mrs clickable">
                    <FormattedMessage numberOfNeedsReview={numberOfNeedsReview} message={this.getIntlMessage("repositories.table.row.needsReview")} />
                </Label>
            </Link>
        );
    },

    /**
     * @param {number} numberRejected
     * @return {XML}
     */
    getRejectedLabel(numberRejected) {
        const repoId = this.props.rowData.id;
        return (<Link onClick={this.updateSearchParamsForRejected.bind(this, repoId)} to='/workbench' className="label-container status-label-container">
            <Label bsStyle="danger" className="mrs clickable">
                <FormattedMessage numberOfRejected={numberRejected} message={this.getIntlMessage("repositories.table.row.rejected")} />
            </Label>
        </Link>);
    },

    /**
     * @return {XML}
     */
    getNeedsTranslationLabel(numberOfNeedsTranslation) {
        const repoId = this.props.rowData.id;
        return (<Link onClick={this.updateSearchParamsForNeedsTranslation.bind(this, repoId)} to='/workbench' className="label-container status-label-container">
            <Label className="mrs clickable">
                <FormattedMessage numberOfNeedsTranslation={numberOfNeedsTranslation} message={this.getIntlMessage("repositories.table.row.needsTranslation")} />
            </Label>
        </Link>);
    },

    /**
     * @return {XML}
     */
    getOkLabel() {
        const repoId = this.props.rowData.id;
        return (<Link onClick={this.updateSearchParamsForRepoDefault.bind(this, repoId)} to='/workbench' className="label-container status-label-container">
            <Label bsStyle="success" className="mrs clickable">
                {this.getIntlMessage("repositories.table.row.ok")}
            </Label>
        </Link>);
    },

    /**
     * @return {XML}
     */
    getStatusLabel() {
        const repoId = this.props.rowData.id;
        let numberOfNeedsReview = this.getNumberOfNeedsReview(repoId);
        let numberOfRejected = this.getNumberOfRejected(repoId);
        let numberOfNeedsTranslation = this.getNumberOfNeedsTranslation(repoId);

        return (
            <span>
                {!numberOfNeedsReview && !numberOfRejected && !numberOfNeedsTranslation ? this.getOkLabel() : ""}
                {numberOfNeedsTranslation ? this.getNeedsTranslationLabel(numberOfNeedsTranslation) : ""}
                {numberOfNeedsReview ? this.getNeedsReviewLabel(numberOfNeedsReview) : ""}
                {numberOfRejected ? this.getRejectedLabel(numberOfRejected) : ""}
            </span>
        );
    },

    /**
     * Handle when progress bar is clicked
     */
    onClickProgressBar() {
        if (!this.state.isActive) {
            this.setState({
                "isActive": true
            });

            this.props.onClickProgressBar(this.props.rowData.id);
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
            descriptionTooltip = <Tooltip>{this.props.rowData.description}</Tooltip>;
        }

        let rowClass = "";
        if (this.state.isActive) {
            rowClass = "repo-active";
        } else if (this.props.isBlurred) {
            rowClass = "repo-blurred";
        }

        let progressLabel = <FormattedNumber value={(this.state.percent / 100).toFixed(3)} style="percent" />;
        let progressClassName = "";
        if (this.state.percent === 0) {
            progressClassName = "zero-progress-bar";
        }

        return (
            <tr className={rowClass}>
                <td className="repo-name" overlay={descriptionTooltip}>
                    <OverlayTrigger placement="right" overlay={descriptionTooltip}>
                        <Link onClick={this.updateSearchParamsForRepoDefault.bind(this, repoId)} to='/workbench'>{this.props.rowData.name}</Link>
                    </OverlayTrigger>
                </td>
                <td>{this.getStatusLabel()}</td>
                <td className="clickable progress-cell" onClick={this.onClickProgressBar}>
                    <ProgressBar now={this.state.percent} label={progressLabel} className={progressClassName} />
                </td>
            </tr>
        );
    }
});
export default RepositoryRow;
