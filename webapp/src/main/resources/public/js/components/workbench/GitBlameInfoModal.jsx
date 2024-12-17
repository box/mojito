import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, Modal} from "react-bootstrap";
import {withAppConfig} from "../../utils/AppConfig";
import TemplateHelper from "../../utils/TemplateHelper";
import {Link} from "react-router";
import md5 from "md5";
import LinkHelper from "../../utils/LinkHelper";
import Locales from "../../utils/Locales";

class GitBlameInfoModal extends React.Component {
    static propTypes() {
        return {
            "show": PropTypes.bool.isRequired,
            "textUnit": PropTypes.object.isRequired,
            "gitBlameWithUsage": PropTypes.object.isRequired,
            "onViewScreenshotClick": PropTypes.func.isRequired
        };
    }

    /**
     * @returns {string} The title of the modal is the name of the text unit
     */
    getTitle = () => {
        return this.props.intl.formatMessage({id: "workbench.gitBlameModal.info"});
    };

    /**
     * @param label  The label for the data to display
     * @param data   The data to display
     * @param direction The direction (ltr/rtl) of the data to display
     * @returns {*}  The row of label:data to display in the modal
     */
    displayInfo = (label, data, direction="ltr") => {
        let gitBlameClass = "";
        if (data == null) {
            gitBlameClass = " git-blame-unused";
            data = "-";
        }

        return (
            <div className={"row git-blame"}>
                <label className={"col-sm-3 git-blame-label"}>{label}</label>
                <div className={"col-sm-9 git-blame-info" + gitBlameClass} dir={direction}>{data}</div>
            </div>
        );
    };

    /**
     * @returns {*} The row of label:link to display in the modal
     */
    displayScreenshotLink = () => {
        const label = this.props.intl.formatMessage({id: "textUnit.gitBlameModal.screenshots"});
        const branchScreenshots = this.getBranchScreenshots();
        if (!branchScreenshots.length) {
            return this.displayInfo(label, null);
        }

        return (
            <div className={"row git-blame"}>
                <label className={"col-sm-3 git-blame-label"}>{label}</label>
                <Link
                    onClick={() => this.props.onViewScreenshotClick(branchScreenshots)}
                    className="col-sm-9 clickable">
                    <FormattedMessage id="textUnit.gitBlameModal.screenshotModalOpen"/>
                </Link>
            </div>
        );
    };

    /**
     * @param labelId  The labelId for the formatted message
     * @param data     The data to display
     * @param direction The direction (ltr/rtl) of the data to display
     * @returns {*}    The row of label:data to display in the modal
     */
    displayInfoWithId = (labelId, data, direction="ltr") => {
        return this.displayInfo(this.props.intl.formatMessage({id: labelId}), data, direction);
    };

    /**
     * @returns {*} Generated content for the text unit information section
     */
    renderTextUnitInfo = () => {
        return this.props.textUnit === null ? "" :
            (
                <div>
                    {this.displayInfoWithId("textUnit.gitBlameModal.repository", this.props.textUnit.getRepositoryName())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.assetPath", this.props.textUnit.getAssetPath())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.id", this.props.textUnit.getName())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.source", this.props.textUnit.getSource())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.target", this.props.textUnit.getTarget(), Locales.getLanguageDirection(this.props.textUnit.getTargetLocale()))}
                    {this.displayInfoWithId("textUnit.gitBlameModal.locale", this.props.textUnit.getTargetLocale())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.created", this.convertDateTime(this.props.textUnit.getTmTextUnitCreatedDate()))}
                    {this.displayInfoWithId("textUnit.gitBlameModal.translated", this.getTranslatedDate())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.pluralForm", this.props.textUnit.getPluralForm())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.pluralFormOther", this.props.textUnit.getPluralFormOther())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.comment", this.props.textUnit.getComment())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.targetComment", this.props.textUnit.getTargetComment())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.location", this.getLocationLinks())}
                    {this.shouldShowThirdPartyTMS() && this.displayInfoWithId("textUnit.gitBlameModal.thirdPartyTMS", this.getThirdPartyLink())}
                    {this.shouldShowCustomMd5() && this.displayInfoWithId("textUnit.gitBlameModal.customMd5", this.getCustomMd5Link())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.introducedBy", this.getIntroducedByLink())}
                </div>
            );
    };

    /**
     * @returns {*} Generated content for the git blame information section
     */
    renderGitBlameInfo = () => {
        return (
            <div>
                {this.displayInfoWithId("textUnit.gitBlameModal.authorName", this.getAuthorName())}
                {this.displayInfoWithId("textUnit.gitBlameModal.authorEmail", this.getAuthorEmail())}
                {this.displayInfoWithId("textUnit.gitBlameModal.commit", this.getCommitLink())}
                {this.displayInfoWithId("textUnit.gitBlameModal.commitDate", this.convertDateTime(this.getCommitTime()))}
            </div>
        )
    };

    /**
     * @returns {*} Generated content for the additional text unit information section
     */
    renderDebugInfo = () => {
        return this.props.textUnit === null ? "" :
            (
                <div className="panel-body">
                    {this.displayInfoWithId("textUnit.gitBlameModal.isVirtual", this.getVirtual())}
                    {this.displayInfo("TmTextUnitId", this.props.textUnit.getTmTextUnitId())}
                    {this.displayInfo("TmTextUnitVariantId", this.props.textUnit.getTmTextUnitVariantId())}
                    {this.displayInfo("TmTextUnitCurrentVariantId", this.props.textUnit.getTmTextUnitCurrentVariantId())}
                    {this.displayInfo("AssetTextUnitId", this.props.textUnit.getAssetTextUnitId())}
                    {this.displayInfo("ThirdPartyTMSId", this.getThirdPartyTextUnitId())}
                    {this.displayInfo("AssetId", this.props.textUnit.getAssetId())}
                    {this.displayInfo("LastSuccessfulAsset\nExtractionId", this.props.textUnit.getLastSuccessfulAssetExtractionId())}
                    {this.displayInfo("AssetExtractionId", this.props.textUnit.getAssetExtractionId())}
                    {this.displayInfo("Branch", this.getBranch())}
                    {this.displayScreenshotLink()}
                </div>
            );
    };

    /**
     * @param property The property of the GitBlame object to retrieve
     * @returns {*}    The value of the given property of the GitBlame object
     */
    getGitBlameProperty = (property) => {
        let value = null;
        if (this.props.gitBlameWithUsage != null && this.props.gitBlameWithUsage["gitBlame"] != null)
            value = this.props.gitBlameWithUsage["gitBlame"][property];
        return value;
    };

    /**
     * @returns {str} The code author of the current text unit
     */
    getAuthorName = () => {
        return this.getGitBlameProperty("authorName");
    };

    /**
     * @returns {str} The email of the code author of the current text unit
     */
    getAuthorEmail = () => {
        return this.getGitBlameProperty("authorEmail");
    };

    /**
     * @returns {str} The commit name of the current text unit
     */
    getCommitName = () => {
        return this.getGitBlameProperty("commitName");
    };

    /**
     * @returns {int} The commit time (in ms) of the current text unit
     */
    getCommitTime = () => {
        return parseInt(this.getGitBlameProperty("commitTime")) * 1000;
    };

    /**
     * @returns {*} A list of usages of the current text unit, or null if there are none
     */
    getUsages = () => {
        if (this.props.gitBlameWithUsage == null || this.props.gitBlameWithUsage["usages"] == null)
            return null;
        return this.props.gitBlameWithUsage["usages"];
    };

    getBranch = () => {
        try {
            return this.props.gitBlameWithUsage.branch.name;
        } catch (e) {
            return " - ";
        }
    };

    getBranchScreenshots = () => {
        try {
            return this.props.gitBlameWithUsage.screenshots;
        } catch (e) {
            return [];
        }
    };

    getThirdPartyTextUnitId = () => {
        try {
            return this.props.gitBlameWithUsage.thirdPartyTextUnitId;
        } catch (e) {
            return " - ";
        }
    };

    getVirtual = () => {
        try {
            return this.props.gitBlameWithUsage.isVirtual.toString();
        } catch (e) {
            return " - ";
        }
    };

    getIntroducedBy = () => {
        try {
            return this.props.gitBlameWithUsage.introducedBy.toString();
        } catch (e) {
            return "-";
        }
    }

    /**
     * Base params are just a subset of text unit properties that can be used inside templates.
     *
     * See getCompoundParams() for params that are computed using the base params and templates coming from the config.
     */
    getBaseParams = () => {
        return {
            textUnitName: this.props.textUnit.getName(),
            textUnitContent: this.props.textUnit.getSource(),
            textUnitNameInSource: this.getTextUnitNameInSource(),
            assetPath: this.props.textUnit.getAssetPath(),
            thirdPartyTextUnitId: this.getThirdPartyTextUnitId(),
            targetLocale: this.props.textUnit.getTargetLocale(),
        };
    };

    /**
     * Compound params are built using templates coming from the config and the base params
     */
    getCompoundParams = (baseParams) => {
        return {
            customMd5: this.getCustomMd5(baseParams)
        };
    };

    /**
     * For links, use base and compound params plus an "encoded" version of each param to be used in the url components.
     */
    getParamsForLinks = () => {
        const baseParams = this.getBaseParams();
        const compoundParams = this.getCompoundParams(baseParams);
        const paramsForLinks = Object.assign({}, baseParams, compoundParams);
        return paramsForLinks;
    };

    getTextUnitNameInSource() {
        const regex = this.getTextUnitNameToTextUnitNameInSourceRegex();
        const match = this.props.textUnit.getName().match(regex);
        return match[1];
    }

    getTextUnitNameToTextUnitNameInSourceRegex() {
        return this.props.textUnit.getPluralForm() == null
            ? this.getTextUnitNameToTextUnitNameInSourceSingular()
            : this.getTextUnitNameToTextUnitNameInSourcePlural();
    }

    getThirdPartyLink = () => {
        return LinkHelper.renderLinkOrLabel(
            this.getThirdPartyUrlTemplate(),
            this.getThirdPartyLabelTemplate(),
            this.getParamsForLinks());
    };

    getCustomMd5Link = () => {
        return LinkHelper.renderLinkOrLabel(
            this.getCustomMd5UrlTemplate(),
            this.getCustomMd5LabelTemplate(),
            this.getParamsForLinks());
    };

    getIntroducedByLink = () => {
        try {
            const parsedUrl = new URL(this.getIntroducedBy());
            return <a href={parsedUrl.toString()}>{parsedUrl.toString()}</a>;
        } catch (error) {
            return this.getIntroducedBy();
        }
    };

    getCustomMd5 = (baseParams) => {
        let renderedTemplate = TemplateHelper.renderTemplate(this.getCustomMd5Template(), baseParams);
        return md5(renderedTemplate);
    };

    /**
     * @returns {*|Array} A list of Location links if configuration is set, or the string set in getLocationDefaultLabel
     */
    getLocationLinks = () => {
        const urlTemplate = this.getLocationUrlTemplate();
        const useUsage = this.getLocationUseUsage();
        let links = [];

        if (urlTemplate === "") {
            links = this.getLocationDefaultLabel();
        } else if (useUsage) {
            links = this.getLocationLinksFromUsages();
        } else {
            links = this.getLocationLink();
        }
        return links;
    };

    /**
     * @returns {*} Creates a link to a single location
     */
    getLocationLink = () => {
        return LinkHelper.renderLinkOrLabel(
            this.getLocationUrlTemplate(),
            this.getLocationLabelTemplate(),
            this.getParamsForLinks());
    };

    /**
     * @returns {Array} Creates a list of links corresponding to the usages
     */
    getLocationLinksFromUsages = () => {
        const extractorPrefixRegex = this.getLocationExtractorPrefixRegex();

        let links = [];
        let usages = this.getUsages();

        if (usages == null) {
            links = this.getLocationDefaultLabel();
        } else {
            for (let usage of usages) {
                if (extractorPrefixRegex !== undefined) {
                    let regExp = new RegExp(extractorPrefixRegex);
                    usage = usage.replace(regExp, "");
                }
                let params = {
                    filePath: this.getFilePathFromUsage(usage),
                    lineNumber: this.getLineNumberFromUsage(usage),
                    textUnitName: this.props.textUnit.getName(),
                    assetPath: this.props.textUnit.getAssetPath(),
                    usage: usage
                };
                let link = LinkHelper.renderLinkOrLabel(this.getLocationUrlTemplate(), this.getLocationLabelTemplate(), params);
                links.push(<div>{link}</div>);
            }
        }
        return links;

    };

    /**
     * @returns {*} Returns the extractorPrefixRegex, if in configuration, or an empty string otherwise
     */
    getLocationExtractorPrefixRegex = () => {
        try {
            return this.props.appConfig.link[this.props.textUnit.getRepositoryName()].location.extractorPrefixRegex;
        } catch (e) {
            return "";
        }
    };

    /**
     * @param usage          Usage to use to extract file path
     * @returns {string | *} File path from given usage
     */
    getFilePathFromUsage = (usage) => {
        return usage.split(':')[0];
    };

    /**
     * @param usage          Usage to use to extract line number
     * @returns {string | *} Line number from given usage
     */
    getLineNumberFromUsage = (usage) => {
        return usage.split(':')[1];
    };

    /**
     * @returns {*} Template for location url, if in configuration, an empty string otherwise.
     */
    getLocationUrlTemplate = () => {
        try {
            return this.props.appConfig.link[this.props.textUnit.getRepositoryName()].location.url;
        } catch (e) {
            return "";
        }
    };

    /**
     * @returns {*} Template for location label, if in configuration, or the string set in getLocationDefaultLabel otherwise
     */
    getLocationLabelTemplate = () => {
        try {
            return this.props.appConfig.link[this.props.textUnit.getRepositoryName()].location.label;
        } catch (e) {
            return this.getLocationDefaultLabel();
        }
    };

    /**
     * @returns {*} The value of useUsage, if in configuration, or false otherwise
     */
    getLocationUseUsage = () => {
        try {
            return this.props.appConfig.link[this.props.textUnit.getRepositoryName()].location.useUsage;
        } catch (e) {
            return false;
        }
    };

    /**
     * @returns {*} The default string for location in case no link configuration is set
     */
    getLocationDefaultLabel = () => {
        const usages = this.getUsages();
        if (usages !== null && usages.length > 0) {
            return usages.join("\n");
        } else {
            return '-';
        }
    };

    /**
     * @returns {*} Says if the Third party TMS info should be displayed or not
     */
    shouldShowThirdPartyTMS = () => {
        try {
            return this.props.appConfig.link[this.props.textUnit.getRepositoryName()].thirdParty !== null;
        } catch (e) {
            return false;
        }
    };

    shouldShowCustomMd5 = () => {
        try {
            return this.props.appConfig.link[this.props.textUnit.getRepositoryName()].customMd5 !== null;
        } catch (e) {
            return false;
        }
    };

    getCustomMd5Template = () => {
        try {
            return this.props.appConfig.link[this.props.textUnit.getRepositoryName()].customMd5.template;
        } catch (e) {
            return;
        }
    };

    getCustomMd5UrlTemplate = () => {
        try {
            return this.props.appConfig.link[this.props.textUnit.getRepositoryName()].customMd5.url;
        } catch (e) {
            return;
        }
    };

    getCustomMd5LabelTemplate = () => {
        let label;

        try {
            label = this.props.appConfig.link[this.props.textUnit.getRepositoryName()].customMd5.label;
        } catch (e) {
        }

        if (!label) {
            label = "${customMd5}";
        }

        return label;
    };

    /**
     * @returns {*} Template for thirdParty url, if in configuration, an empty string otherwise.
     *
     * TODO this is not true, it returns undefined if thirdParty is present but not url, this comment applies to all
     * other similar functions too
     */
    getThirdPartyUrlTemplate = () => {
        try {
            return this.props.appConfig.link[this.props.textUnit.getRepositoryName()].thirdParty.url;
        } catch (e) {
            return "";
        }
    };

    /**
     * @returns {*} Template for thirdParty label, if in configuration, or the string set in getThirdPartyDefaultLabel otherwise
     */
    getThirdPartyLabelTemplate = () => {
        try {
            return this.props.appConfig.link[this.props.textUnit.getRepositoryName()].thirdParty.label;
        } catch (e) {
            return null;
        }
    };

    /**
     * @returns {*} Regex to tranform the text unit name into the text unit name as it appear in source code,
     * if in configuration, an empty string otherwise.
     */
    getTextUnitNameToTextUnitNameInSourceSingular = () => {
        try {
            return this.props.appConfig.link[this.props.textUnit.getRepositoryName()].textUnitNameToTextUnitNameInSource.singular;
        } catch (e) {
            return "(.*)";
        }
    };

    /**
     * @returns {*} Regex to tranform text unit name into text unit string in code, if in configuration, default
     * to exp to remove plural otherwise.
     */
    getTextUnitNameToTextUnitNameInSourcePlural = () => {
        try {
            return this.props.appConfig.link[this.props.textUnit.getRepositoryName()].textUnitNameToTextUnitNameInSource.plural;
        } catch (e) {
            return "(.*) ?_(zero|one|two|few|many|other)$";
        }
    };

    /**
     * @returns {*} Creates a link to current commit if there is one, or value of getCommitDefaultLabel if there is none
     */
    getCommitLink = () => {
        const urlTemplate = this.getCommitUrlTemplate();
        const commit = this.getCommitName();
        let link = this.getCommitDefaultLabel();

        if (commit !== null && urlTemplate !== "") {
            let params = {commit: commit};
            link = LinkHelper.renderLinkOrLabel(urlTemplate, this.getCommitLabelTemplate(), params);
        }

        return link;
    };

    /**
     * @returns {*} Template for commit url, if in configuration, an empty string otherwise.
     */
    getCommitUrlTemplate = () => {
        try {
            return this.props.appConfig.link[this.props.textUnit.getRepositoryName()].commit.url;
        } catch (e) {
            return ""
        }
    };

    /**
     * @returns {*} Template for commit label, if in configuration, or the string set in getCommitDefaultLabel otherwise
     */
    getCommitLabelTemplate = () => {
        try {
            return this.props.appConfig.link[this.props.textUnit.getRepositoryName()].commit.label;
        } catch (e) {
            return this.getCommitDefaultLabel();
        }
    };

    /**
     * @returns {*} The default string for commit in case no link configuration is set
     */
    getCommitDefaultLabel = () => {
        return this.getCommitName();
    };

    /**
     * @returns {*} The translated date if the text unit has been translated
     */
    getTranslatedDate = () => {
        if (this.props.textUnit.getTmTextUnitCurrentVariantId() != null) {
            return this.convertDateTime(this.props.textUnit.getCreatedDate());
        }
    };

    /**
     * @param date   An integer representing a datetime
     * @returns {*}  Human readable version of the given datetime
     *
     */
    convertDateTime = (date) => {
        if (date === null || isNaN(date)) {
            return null;
        }

        let options = {
            year: 'numeric', month: 'numeric', day: 'numeric',
            hour: 'numeric', minute: 'numeric'
        };
        return (this.props.intl.formatDate(date, options));
    };

    /**
     * Closes the modal and calls the parent action handler to mark the modal as closed
     */
    closeModal = () => {
        this.props.onCloseModal();
    };

    render() {
        return (
            <Modal className={"git-blame-modal"} show={this.props.show} onHide={this.closeModal}>
                <Modal.Header closeButton>
                    <Modal.Title>{this.getTitle()}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div className={"row"}>
                        <div className={"col-sm-4"}><h4><FormattedMessage id={"textUnit.gitBlameModal.textUnit"}/></h4>
                        </div>
                    </div>
                    {this.renderTextUnitInfo()}
                    <hr/>
                    <div className={"row"}>
                        <div className={"col-sm-4"}><h4><FormattedMessage id={"textUnit.gitBlameModal.gitBlame"}/></h4>
                        </div>
                        <div className={"col-sm-8"}>
                            {this.props.loading ? (<span className="glyphicon glyphicon-refresh spinning"/>) : ""}
                        </div>
                    </div>
                    {this.renderGitBlameInfo()}
                    <hr/>
                    <div className={"row"}>
                        <div className={"col-sm-4"}><h4><FormattedMessage id={"textUnit.gitBlameModal.more"}/></h4>
                        </div>
                    </div>
                    {this.renderDebugInfo()}
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="primary" onClick={this.closeModal}>
                        <FormattedMessage id={"textUnit.gitBlameModal.close"}/>
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

export default withAppConfig(injectIntl(GitBlameInfoModal));
