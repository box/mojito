import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, Modal} from "react-bootstrap";
import {withAppConfig} from "../../utils/AppConfig";
import LinkHelper from "../../utils/LinkHelper";


let GitBlameInfoModal = React.createClass({

    propTypes() {
        return {
            "show": PropTypes.bool.isRequired,
            "textUnit": PropTypes.object.isRequired,
            "gitBlameWithUsage": PropTypes.object.isRequired
        };
    },

    /**
     * @returns {string} The title of the modal is the name of the text unit
     */
    getTitle() {
        return this.props.intl.formatMessage({ id: "workbench.gitBlameModal.info" });
    },

    /**
     * @param label  The label for the data to display
     * @param data   The data to display
     * @returns {*}  The row of label:data to display in the modal
     */
    displayInfo(label, data) {
        let gitBlameClass = "";
        if (data == null) {
            gitBlameClass = " git-blame-unused";
            data = "-";
        }

        return (
            <div className={"row git-blame"}>
                <label className={"col-sm-3 git-blame-label"}>{label}</label>
                <div className={"col-sm-9 git-blame-info" + gitBlameClass}>{data}</div>
            </div>
        );
    },

    /**
     * @param labelId  The labelId for the formatted message
     * @param data     The data to display
     * @returns {*}    The row of label:data to display in the modal
     */
    displayInfoWithId(labelId, data) {
        return this.displayInfo(this.props.intl.formatMessage({ id: labelId }), data);
    },

    /**
     * @returns {*} Generated content for the text unit information section
     */
    renderTextUnitInfo() {
        return this.props.textUnit === null ? "" :
            (
                <div>
                    {this.displayInfoWithId("textUnit.gitBlameModal.repository", this.props.textUnit.getRepositoryName())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.assetPath", this.props.textUnit.getAssetPath())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.id", this.props.textUnit.getName())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.source", this.props.textUnit.getSource())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.target", this.props.textUnit.getTarget())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.locale", this.props.textUnit.getTargetLocale())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.created", this.convertDateTime(this.props.textUnit.getTmTextUnitCreatedDate()))}
                    {this.displayInfoWithId("textUnit.gitBlameModal.translated", this.convertDateTime(this.props.textUnit.getCreatedDate()))}
                    {this.displayInfoWithId("textUnit.gitBlameModal.pluralForm", this.props.textUnit.getPluralForm())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.pluralFormOther", this.props.textUnit.getPluralFormOther())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.comment", this.props.textUnit.getComment())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.targetComment", this.props.textUnit.getTargetComment())}
                    {this.displayInfoWithId("textUnit.gitBlameModal.location", this.getLocationLinks())}
                </div>
            );
    },

    /**
     * @returns {*} Generated content for the git blame information section
     */
    renderGitBlameInfo() {
        return (
            <div>
                {this.displayInfoWithId("textUnit.gitBlameModal.authorName", this.getAuthorName())}
                {this.displayInfoWithId("textUnit.gitBlameModal.authorEmail", this.getAuthorEmail())}
                {this.displayInfoWithId("textUnit.gitBlameModal.commit", this.getCommitLink())}
                {this.displayInfoWithId("textUnit.gitBlameModal.commitDate", this.convertDateTime(this.getCommitTime()))}
            </div>
        )
    },

    /**
     * @returns {*} Generated content for the additional text unit information section
     */
    renderDebugInfo() {
        return this.props.textUnit === null ? "" :
            (
                <div className="panel-body">
                    {this.displayInfo("TmTextUnitId", this.props.textUnit.getTmTextUnitId())}
                    {this.displayInfo("TmTextUnitVariantId", this.props.textUnit.getTmTextUnitVariantId())}
                    {this.displayInfo("TmTextUnitCurrentVariantId", this.props.textUnit.getTmTextUnitCurrentVariantId())}
                    {this.displayInfo("AssetTextUnitId", this.props.textUnit.getAssetId())}
                    {this.displayInfo("LastSuccessfulAsset\nExtractionId", this.props.textUnit.getLastSuccessfulAssetExtractionId())}
                    {this.displayInfo("AssetExtractionId", this.props.textUnit.getAssetExtractionId())}
                    {this.displayInfo("Branch", this.getBranch())}
                </div>
            );
    },

    /**
     * @param property The property of the GitBlame object to retrieve
     * @returns {*}    The value of the given property of the GitBlame object
     */
    getGitBlameProperty(property) {
        let value = null;
        if (this.props.gitBlameWithUsage != null && this.props.gitBlameWithUsage["gitBlame"] != null)
            value = this.props.gitBlameWithUsage["gitBlame"][property];
        return value;
    },

    /**
     * @returns {str} The code author of the current text unit
     */
    getAuthorName() {
        return this.getGitBlameProperty("authorName");
    },

    /**
     * @returns {str} The email of the code author of the current text unit
     */
    getAuthorEmail() {
        return this.getGitBlameProperty("authorEmail");
    },

    /**
     * @returns {str} The commit name of the current text unit
     */
    getCommitName() {
        return this.getGitBlameProperty("commitName");
    },

    /**
     * @returns {int} The commit time (in ms) of the current text unit
     */
    getCommitTime() {
        return parseInt(this.getGitBlameProperty("commitTime")) * 1000;
    },

    /**
     * @returns {*} A list of usages of the current text unit, or null if there are none
     */
    getUsages() {
        if (this.props.gitBlameWithUsage == null || this.props.gitBlameWithUsage["usages"] == null)
            return null;
        return this.props.gitBlameWithUsage["usages"];
    },

    getBranch() {
        try {
            return this.props.gitBlameWithUsage.branch.name;
        } catch(e) {
            return " - ";
        }
    },

    /**
     * @returns {*|Array} A list of Location links if configuration is set, or the string set in getLocationDefaultLabel
     */
    getLocationLinks() {
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
    },

    /**
     * @returns {*} Creates a link to a single location
     */
    getLocationLink() {
        const urlTemplate = this.getLocationUrlTemplate();
        const labelTemplate = this.getLocationLabelTemplate();

        let params = {
            textUnitName: this.props.textUnit.getName(),
            assetPath: this.props.textUnit.getAssetPath(),
            textUsageNamePrefix: this.props.textUnit.getName().split("_")[0],
        };
        let url = LinkHelper.getLink(urlTemplate, params);
        let label = LinkHelper.getLink(labelTemplate, params);
        return <a href={url}>{label}</a>;
    },

    /**
     * @returns {Array} Creates a list of links corresponding to the usages
     */
    getLocationLinksFromUsages() {
        const urlTemplate = this.getLocationUrlTemplate();
        const labelTemplate = this.getLocationLabelTemplate();
        const extractorPrefix = this.getLocationExtractorPrefix();

        let links = [];
        let usages = this.getUsages();

        if (usages == null) {
            links = this.getLocationDefaultLabel();
        } else {
            for (let usage of usages) {
                if (extractorPrefix !== undefined) {
                    usage = usage.replace(extractorPrefix, "");
                }
                let params = {
                    filePath: this.getFilePathFromUsage(usage),
                    lineNumber: this.getLineNumberFromUsage(usage),
                    textUnitName: this.props.textUnit.getName(),
                    assetPath: this.props.textUnit.getAssetPath(),
                    usage: usage
                };
                let url = LinkHelper.getLink(urlTemplate, params);
                let label = LinkHelper.getLink(labelTemplate, params);
                links.push(<div key={usage}><a href={url}>{label}</a></div>);
            }
        }
        return links;

    },

    /**
     * @returns {*} Returns the extractorPrefix, if in configuration, or an empty string otherwise
     */
    getLocationExtractorPrefix() {
        try {
            return this.props.appConfig.link.link[this.props.textUnit.getRepositoryName()].location.extractorPrefix;
        } catch (e) {
            return "";
        }
    },

    /**
     * @param usage          Usage to use to extract file path
     * @returns {string | *} File path from given usage
     */
    getFilePathFromUsage(usage) {
        return usage.split(':')[0];
    },

    /**
     * @param usage          Usage to use to extract line number
     * @returns {string | *} Line number from given usage
     */
    getLineNumberFromUsage(usage) {
        return usage.split(':')[1];
    },

    /**
     * @returns {*} Template for location url, if in configuration, an empty string otherwise.
     */
    getLocationUrlTemplate() {
        try {
            return this.props.appConfig.link.link[this.props.textUnit.getRepositoryName()].location.url;
        } catch (e) {
            return "";
        }
    },

    /**
     * @returns {*} Template for location label, if in configuration, or the string set in getLocationDefaultLabel otherwise
     */
    getLocationLabelTemplate() {
        try {
            return this.props.appConfig.link.link[this.props.textUnit.getRepositoryName()].location.label;
        } catch (e) {
            return this.getLocationDefaultLabel();
        }
    },

    /**
     * @returns {*} The value of useUsage, if in configuration, or false otherwise
     */
    getLocationUseUsage() {
        try {
            return this.props.appConfig.link.link[this.props.textUnit.getRepositoryName()].location.useUsage;
        } catch (e) {
            return false;
        }
    },

    /**
     * @returns {*} The default string for location in case no link configuration is set
     */
    getLocationDefaultLabel() {
        const usages = this.getUsages();
        if (this.getLocationUseUsage() && usages !== null) {
            return usages.join("\n");
        } else {
            return '-';
        }
    },

    /**
     * @returns {*} Creates a link to current commit if there is one, or value of getCommitDefaultLabel if there is none
     */
    getCommitLink() {
        const urlTemplate = this.getCommitUrlTemplate();
        const labelTemplate = this.getCommitLabelTemplate();
        const commit = this.getCommitName();
        let link = this.getCommitDefaultLabel();

        if (commit !== null && urlTemplate !== "") {
            let params = {commit: commit};
            let url = LinkHelper.getLink(urlTemplate, params);
            let label = LinkHelper.getLink(labelTemplate, params);
            link = (<a href={url}>{label}</a>);
        }

        return link;
    },

    /**
     * @returns {*} Template for commit url, if in configuration, an empty string otherwise.
     */
    getCommitUrlTemplate() {
        try {
            return this.props.appConfig.link.link[this.props.textUnit.getRepositoryName()].commit.url;
        } catch (e) {
            return ""
        }
    },

    /**
     * @returns {*} Template for commit label, if in configuration, or the string set in getCommitDefaultLabel otherwise
     */
    getCommitLabelTemplate() {
        try {
            return this.props.appConfig.link.link[this.props.textUnit.getRepositoryName()].commit.label;
        } catch (e) {
            return this.getCommitDefaultLabel();
        }
    },

    /**
     * @returns {*} The default string for commit in case no link configuration is set
     */
    getCommitDefaultLabel() {
        return this.getCommitName();
    },

    /**
     * @param date   An integer representing a datetime
     * @returns {*}  Human readable version of the given datetime
     *
     */
    convertDateTime(date) {
        if (date === null || isNaN(date)) {
            return null;
        }

        let options = {
            year: 'numeric', month: 'numeric', day: 'numeric',
            hour: 'numeric', minute: 'numeric'
        };
        return (this.props.intl.formatDate(date, options));
    },

    /**
     * Closes the modal and calls the parent action handler to mark the modal as closed
     */
    closeModal() {
        this.props.onCloseModal();
    },

    render() {
        return (
            <Modal className={"git-blame-modal"} show={this.props.show} onHide={this.closeModal}>
                <Modal.Header closeButton>
                    <Modal.Title>{this.getTitle()}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div className={"row"}>
                        <div className={"col-sm-4"}><h4><FormattedMessage id={"textUnit.gitBlameModal.textUnit"}/></h4></div>
                    </div>
                    {this.renderTextUnitInfo()}
                    <hr/>
                    <div className={"row"}>
                        <div className={"col-sm-4"}><h4><FormattedMessage id={"textUnit.gitBlameModal.gitBlame"}/></h4></div>
                        <div className={"col-sm-8"}>
                            {this.props.loading ? (<span className="glyphicon glyphicon-refresh spinning"/>) : ""}
                        </div>
                    </div>
                    {this.renderGitBlameInfo()}
                    <hr/>
                    <div className={"row"}>
                        <div className={"col-sm-4"}><h4><FormattedMessage id={"textUnit.gitBlameModal.more"}/></h4></div>
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
});

export default withAppConfig(injectIntl(GitBlameInfoModal));
