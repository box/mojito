import React from "react";
import AltContainer from "alt-container";
import {
    Alert,
    Button,
    Checkbox,
    ControlLabel,
    Form,
    FormControl,
    FormGroup,
    HelpBlock
} from "react-bootstrap";
import {FormattedMessage} from "react-intl";
import RepositoryActions from "../../actions/RepositoryActions";
import RepositoryStore from "../../stores/RepositoryStore";
import RepositoryLocale from "../../sdk/entity/RepositoryLocale";
import RepositoryAiTranslateClient from "../../sdk/RepositoryAiTranslateClient";
import PollableTaskClient from "../../sdk/PollableTaskClient";

class RepositoryAiTranslatePageContainer extends React.Component {

    render() {
        return (
            <AltContainer store={RepositoryStore}>
                <RepositoryAiTranslatePage />
            </AltContainer>
        );
    }
}

class RepositoryAiTranslatePage extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            selectedRepositoryId: "",
            selectedLocales: [],
            sourceTextMaxCount: 100,
            textUnitIdsRaw: "",
            useModel: "gpt-4.1",
            promptSuffix: "",
            relatedStrings: "NONE",
            translateType: "TARGET_ONLY_NEW",
            statusFilter: "FOR_TRANSLATION",
            importStatus: "REVIEW_NEEDED",
            downloadReport: false,
            timeoutSeconds: "",
            dryRun: false,
            isSubmitting: false,
            isWaitingForCompletion: false,
            jobError: null,
            pollableTask: null,
            reportLocaleUrls: [],
            reportDownloads: [],
            isFetchingReport: false,
            reportError: null
        };
        this.activeObjectUrls = [];
    }

    componentWillUnmount() {
        this.cleanupObjectUrls();
    }

    cleanupObjectUrls() {
        this.activeObjectUrls.forEach(url => URL.revokeObjectURL(url));
        this.activeObjectUrls = [];
    }

    getRepositories() {
        return this.props.repositories || [];
    }

    getSelectedRepository() {
        const repositories = this.getRepositories();
        const {selectedRepositoryId} = this.state;
        if (!selectedRepositoryId) {
            return null;
        }
        const repositoryIdAsNumber = Number(selectedRepositoryId);
        if (Number.isNaN(repositoryIdAsNumber)) {
            return null;
        }
        return repositories.find(repository => repository.id === repositoryIdAsNumber) || null;
    }

    getAvailableLocales() {
        const repository = this.getSelectedRepository();
        if (!repository || !repository.repositoryLocales) {
            return [];
        }
        return repository.repositoryLocales
            .filter(repositoryLocale => !RepositoryLocale.isRootLocale(repositoryLocale))
            .map(repositoryLocale => repositoryLocale.locale.bcp47Tag)
            .sort();
    }

    toggleLocale(locale) {
        this.setState(previousState => {
            const selectedLocales = new Set(previousState.selectedLocales);
            if (selectedLocales.has(locale)) {
                selectedLocales.delete(locale);
            } else {
                selectedLocales.add(locale);
            }
            return {selectedLocales: Array.from(selectedLocales)};
        });
    }

    handleRepositoryChange(event) {
        const selectedRepositoryId = event.target.value;
        this.cleanupObjectUrls();
        this.setState({
            selectedRepositoryId,
            selectedLocales: [],
            pollableTask: null,
            reportLocaleUrls: [],
            reportDownloads: [],
            reportError: null,
            jobError: null
        });
    }

    handleInputChange(event) {
        const {name, value, type, checked} = event.target;
        this.setState({
            [name]: type === "checkbox" ? checked : value
        });
    }

    parseTextUnitIds(rawValue) {
        if (!rawValue || !rawValue.trim()) {
            return null;
        }
        const ids = rawValue
            .split(/[,\n\s]+/)
            .map(entry => entry.trim())
            .filter(entry => entry.length > 0)
            .map(entry => Number(entry));
        if (ids.some(Number.isNaN)) {
            throw new Error("invalidTextUnitIds");
        }
        return ids;
    }

    parseInteger(value, allowEmpty = true) {
        if (value === null || value === undefined) {
            return null;
        }
        if (value === "") {
            if (allowEmpty) {
                return null;
            }
            throw new Error("invalidNumber");
        }
        const parsed = Number(value);
        if (!Number.isFinite(parsed) || !Number.isInteger(parsed)) {
            throw new Error("invalidNumber");
        }
        return parsed;
    }

    buildRequestPayload() {
        const repository = this.getSelectedRepository();
        if (!repository) {
            throw new Error("missingRepository");
        }

        const sourceTextMaxCount = this.parseInteger(this.state.sourceTextMaxCount, false);
        const timeoutSeconds = this.parseInteger(this.state.timeoutSeconds, true);
        const tmTextUnitIds = this.parseTextUnitIds(this.state.textUnitIdsRaw);

        if (sourceTextMaxCount <= 0) {
            throw new Error("invalidNumber");
        }

        if (timeoutSeconds !== null && timeoutSeconds < 0) {
            throw new Error("invalidNumber");
        }

        return {
            repositoryName: repository.name,
            targetBcp47tags: this.state.selectedLocales.length ? this.state.selectedLocales : null,
            sourceTextMaxCountPerLocale: sourceTextMaxCount,
            tmTextUnitIds,
            useBatch: false,
            useModel: this.state.useModel || null,
            promptSuffix: this.state.promptSuffix || null,
            relatedStringsType: this.state.relatedStrings,
            translateType: this.state.translateType,
            statusFilter: this.state.statusFilter,
            importStatus: this.state.importStatus,
            glossaryName: null,
            glossaryTermSource: null,
            glossaryTermSourceDescription: null,
            glossaryTermTarget: null,
            glossaryTermTargetDescription: null,
            glossaryTermDoNotTranslate: false,
            glossaryTermCaseSensitive: false,
            glossaryOnlyMatchedTextUnits: false,
            dryRun: this.state.dryRun,
            timeoutSeconds
        };
    }

    submitJob(event) {
        event.preventDefault();
        this.cleanupObjectUrls();

        let requestPayload;
        try {
            requestPayload = this.buildRequestPayload();
        } catch (error) {
            let jobErrorMessage;
            switch (error.message) {
                case "missingRepository":
                    jobErrorMessage = "aiTranslate.error.repository";
                    break;
                case "invalidTextUnitIds":
                    jobErrorMessage = "aiTranslate.error.textUnitIds";
                    break;
                case "invalidNumber":
                    jobErrorMessage = "aiTranslate.error.number";
                    break;
                default:
                    jobErrorMessage = "aiTranslate.error.generic";
            }
            this.setState({jobError: jobErrorMessage});
            return;
        }

        this.setState({
            isSubmitting: true,
            isWaitingForCompletion: false,
            jobError: null,
            pollableTask: null,
            reportLocaleUrls: [],
            reportDownloads: [],
            reportError: null
        });

        RepositoryAiTranslateClient.translateRepository(requestPayload)
            .then(response => {
                const pollableTask = response.pollableTask;
                if (!pollableTask || !pollableTask.id) {
                    throw new Error("missingPollableTask");
                }
                this.setState({
                    pollableTask,
                    isWaitingForCompletion: true
                });
                return PollableTaskClient.waitForPollableTaskToFinish(pollableTask.id, null);
            })
            .then(pollableTask => {
                this.setState({
                    pollableTask,
                    isSubmitting: false,
                    isWaitingForCompletion: false
                });
                if (pollableTask.errorMessage) {
                    this.setState({jobError: pollableTask.errorMessage});
                    return;
                }
                if (this.state.downloadReport) {
                    this.fetchReport(pollableTask.id);
                }
            })
            .catch(error => {
                let jobErrorMessage = "aiTranslate.error.generic";
                if (error.message === "missingPollableTask") {
                    jobErrorMessage = "aiTranslate.error.pollable";
                }
                if (error.response) {
                    jobErrorMessage = `${error.response.status} ${error.response.statusText}`;
                }
                this.setState({
                    jobError: jobErrorMessage,
                    isSubmitting: false,
                    isWaitingForCompletion: false
                });
            });
    }

    fetchReport(pollableTaskId) {
        this.setState({
            isFetchingReport: true,
            reportError: null,
            reportLocaleUrls: [],
            reportDownloads: []
        });
        RepositoryAiTranslateClient.getReport(pollableTaskId)
            .then(report => {
                const reportLocaleUrls = report.reportLocaleUrls || [];
                this.setState({reportLocaleUrls});
                return Promise.all(reportLocaleUrls.map(filename =>
                    RepositoryAiTranslateClient.getReportLocale(filename).then(localeResponse => ({
                        filename,
                        content: localeResponse.content
                    }))
                ));
            })
            .then(results => {
                this.cleanupObjectUrls();
                const downloads = results.map(result => {
                    const locale = result.filename.split("/").pop();
                    const blob = new Blob([result.content], {type: "application/json"});
                    const href = URL.createObjectURL(blob);
                    this.activeObjectUrls.push(href);
                    return {
                        locale,
                        href,
                        filename: `${pollableTaskId}-${locale}.json`
                    };
                });
                this.setState({
                    reportDownloads: downloads,
                    isFetchingReport: false
                });
            })
            .catch(() => {
                this.setState({
                    reportError: "aiTranslate.error.report",
                    isFetchingReport: false
                });
            });
    }

    renderRepositorySelect() {
        const repositories = this.getRepositories();
        return (
            <FormGroup controlId="repositorySelect">
                <ControlLabel><FormattedMessage id="aiTranslate.repository"/></ControlLabel>
                <FormControl
                    componentClass="select"
                    value={this.state.selectedRepositoryId}
                    onChange={(event) => this.handleRepositoryChange(event)}
                    disabled={this.state.isSubmitting || this.state.isWaitingForCompletion}
                >
                    <option value="">
                        {""}
                    </option>
                    {repositories.map(repository => (
                        <option key={repository.id} value={repository.id}>{repository.name}</option>
                    ))}
                </FormControl>
                <HelpBlock><FormattedMessage id="aiTranslate.repository.help"/></HelpBlock>
            </FormGroup>
        );
    }

    renderLocalesSection() {
        const availableLocales = this.getAvailableLocales();
        if (availableLocales.length === 0) {
            return null;
        }
        return (
            <FormGroup controlId="localeCheckboxes">
                <ControlLabel><FormattedMessage id="aiTranslate.locales"/></ControlLabel>
                <div>
                    {availableLocales.map(locale => (
                        <Checkbox
                            key={locale}
                            checked={this.state.selectedLocales.indexOf(locale) !== -1}
                            onChange={() => this.toggleLocale(locale)}
                            disabled={this.state.isSubmitting || this.state.isWaitingForCompletion}
                        >
                            {locale}
                        </Checkbox>
                    ))}
                </div>
                <HelpBlock><FormattedMessage id="aiTranslate.locales.help"/></HelpBlock>
            </FormGroup>
        );
    }

    renderNumericInput(id, labelId, stateKey, helpId = null, minValue = 1) {
        return (
            <FormGroup controlId={id}>
                <ControlLabel><FormattedMessage id={labelId}/></ControlLabel>
                <FormControl
                    type="number"
                    min={minValue}
                    value={this.state[stateKey]}
                    name={stateKey}
                    onChange={(event) => this.handleInputChange(event)}
                    disabled={this.state.isSubmitting || this.state.isWaitingForCompletion}
                />
                {helpId && <HelpBlock><FormattedMessage id={helpId}/></HelpBlock>}
            </FormGroup>
        );
    }

    renderTextInput(id, labelId, stateKey, helpId = null) {
        return (
            <FormGroup controlId={id}>
                <ControlLabel><FormattedMessage id={labelId}/></ControlLabel>
                <FormControl
                    type="text"
                    value={this.state[stateKey]}
                    name={stateKey}
                    onChange={(event) => this.handleInputChange(event)}
                    disabled={this.state.isSubmitting || this.state.isWaitingForCompletion}
                />
                {helpId && <HelpBlock><FormattedMessage id={helpId}/></HelpBlock>}
            </FormGroup>
        );
    }

    renderTextarea(id, labelId, stateKey, helpId) {
        return (
            <FormGroup controlId={id}>
                <ControlLabel><FormattedMessage id={labelId}/></ControlLabel>
                <FormControl
                    componentClass="textarea"
                    rows={3}
                    value={this.state[stateKey]}
                    name={stateKey}
                    onChange={(event) => this.handleInputChange(event)}
                    disabled={this.state.isSubmitting || this.state.isWaitingForCompletion}
                />
                <HelpBlock><FormattedMessage id={helpId}/></HelpBlock>
            </FormGroup>
        );
    }

    renderSelect(id, labelId, stateKey, options) {
        return (
            <FormGroup controlId={id}>
                <ControlLabel><FormattedMessage id={labelId}/></ControlLabel>
                <FormControl
                    componentClass="select"
                    value={this.state[stateKey]}
                    name={stateKey}
                    onChange={(event) => this.handleInputChange(event)}
                    disabled={this.state.isSubmitting || this.state.isWaitingForCompletion}
                >
                    {options.map(option => (
                        <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                </FormControl>
            </FormGroup>
        );
    }

    renderReportDownloads() {
        if (!this.state.reportDownloads.length) {
            return null;
        }
        return (
            <div className="mtm">
                <h4><FormattedMessage id="aiTranslate.report.title"/></h4>
                <ul>
                    {this.state.reportDownloads.map(download => (
                        <li key={download.locale}>
                            <a href={download.href} download={download.filename}>
                                <FormattedMessage
                                    id="aiTranslate.report.download"
                                    values={{locale: download.locale}}
                                />
                            </a>
                        </li>
                    ))}
                </ul>
            </div>
        );
    }

    render() {
        const relatedStringsOptions = [
            {value: "NONE", label: "NONE"},
            {value: "USAGES", label: "USAGES"},
            {value: "ID_PREFIX", label: "ID_PREFIX"}
        ];

        const translateTypeOptions = [
            {value: "WITH_REVIEW", label: "WITH_REVIEW"},
            {value: "TARGET_ONLY", label: "TARGET_ONLY"},
            {value: "TARGET_ONLY_NEW", label: "TARGET_ONLY_NEW"}
        ];

        const statusFilterOptions = [
            {value: "FOR_TRANSLATION", label: "FOR_TRANSLATION"},
            {value: "ALL", label: "ALL"}
        ];

        const importStatusOptions = [
            {value: "REVIEW_NEEDED", label: "REVIEW_NEEDED"},
            {value: "ACCEPTED", label: "ACCEPTED"},
            {value: "TRANSLATION_NEEDED", label: "TRANSLATION_NEEDED"}
        ];

        const disableForm = this.state.isSubmitting || this.state.isWaitingForCompletion;

        return (
            <div className="mtn">
                <h2><FormattedMessage id="aiTranslate.title"/></h2>
                <p><FormattedMessage id="aiTranslate.description"/></p>
                <Form onSubmit={(event) => this.submitJob(event)}>
                    {this.renderRepositorySelect()}
                    {this.renderLocalesSection()}
                    {this.renderNumericInput("sourceTextMaxCount", "aiTranslate.sourceTextMaxCount", "sourceTextMaxCount", "aiTranslate.sourceTextMaxCount.help")}
                    {this.renderTextarea("textUnitIds", "aiTranslate.textUnitIds", "textUnitIdsRaw", "aiTranslate.textUnitIds.help")}
                    {this.renderTextInput("useModel", "aiTranslate.useModel", "useModel", "aiTranslate.useModel.help")}
                    {this.renderTextarea("promptSuffix", "aiTranslate.promptSuffix", "promptSuffix", "aiTranslate.promptSuffix.help")}
                    {this.renderSelect("relatedStrings", "aiTranslate.relatedStrings", "relatedStrings", relatedStringsOptions)}
                    {this.renderSelect("translateType", "aiTranslate.translateType", "translateType", translateTypeOptions)}
                    {this.renderSelect("statusFilter", "aiTranslate.statusFilter", "statusFilter", statusFilterOptions)}
                    {this.renderSelect("importStatus", "aiTranslate.importStatus", "importStatus", importStatusOptions)}
                    {this.renderNumericInput("timeoutSeconds", "aiTranslate.timeoutSeconds", "timeoutSeconds", "aiTranslate.timeoutSeconds.help", 0)}
                    <Checkbox
                        name="downloadReport"
                        checked={this.state.downloadReport}
                        onChange={(event) => this.handleInputChange(event)}
                        disabled={disableForm}
                    >
                        <FormattedMessage id="aiTranslate.downloadReport"/>
                    </Checkbox>
                    <Checkbox
                        name="dryRun"
                        checked={this.state.dryRun}
                        onChange={(event) => this.handleInputChange(event)}
                        disabled={disableForm}
                    >
                        <FormattedMessage id="aiTranslate.dryRun"/>
                    </Checkbox>

                    {this.state.jobError &&
                        <Alert bsStyle="danger" className="mtm">
                            <FormattedMessage id={this.state.jobError} defaultMessage={this.state.jobError}/>
                        </Alert>
                    }

                    {this.state.pollableTask && !this.state.jobError && this.state.pollableTask.isAllFinished &&
                        <Alert bsStyle="success" className="mtm">
                            <FormattedMessage
                                id="aiTranslate.success"
                                values={{pollableTaskId: this.state.pollableTask.id}}
                            />
                        </Alert>
                    }

                    {this.state.isWaitingForCompletion &&
                        <Alert bsStyle="info" className="mtm">
                            <FormattedMessage
                                id="aiTranslate.waiting"
                                values={{pollableTaskId: this.state.pollableTask ? this.state.pollableTask.id : ""}}
                            />
                        </Alert>
                    }

                    {this.state.reportError &&
                        <Alert bsStyle="danger" className="mtm">
                            <FormattedMessage id={this.state.reportError}/>
                        </Alert>
                    }

                    {this.state.isFetchingReport &&
                        <Alert bsStyle="info" className="mtm">
                            <FormattedMessage id="aiTranslate.report.fetching"/>
                        </Alert>
                    }

                    {this.renderReportDownloads()}

                    <Button
                        type="submit"
                        bsStyle="primary"
                        disabled={disableForm}
                    >
                        {disableForm ?
                            <span><span className="glyphicon glyphicon-refresh spinning mrs"/><FormattedMessage id="aiTranslate.submitting"/></span>
                            : <FormattedMessage id="aiTranslate.submit"/>}
                    </Button>
                </Form>
            </div>
        );
    }
}

export default RepositoryAiTranslatePageContainer;
