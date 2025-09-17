import PropTypes from 'prop-types';
import React from "react";
import {Alert, Button, ControlLabel, FormGroup, HelpBlock, Modal} from "react-bootstrap";
import {FormattedMessage, injectIntl} from "react-intl";
import TextUnitClient from "../../sdk/TextUnitClient";
import {csvToObjects} from "../../utils/CsvParser";

class ImportSearchResultsModal extends React.Component {
    static propTypes = {
        "show": PropTypes.bool.isRequired,
        "onClose": PropTypes.func.isRequired,
    };

    constructor(props) {
        super(props);
        this.fileInputRef = React.createRef();
        this.state = this.getInitialState();
    }

    getInitialState() {
        return {
            fileName: '',
            format: null,
            parsedTextUnits: [],
            validationErrors: [],
            errorMessage: null,
            isImporting: false,
            fileError: null,
            isDragging: false,
        };
    }

    componentDidUpdate(prevProps) {
        if (!prevProps.show && this.props.show) {
            this.resetState();
        }
    }

    resetState() {
        this.setState(this.getInitialState(), () => {
            if (this.fileInputRef.current) {
                this.fileInputRef.current.value = '';
            }
        });
    }

    onFileChange(event) {
        const file = event.target.files && event.target.files[0];
        if (!file) {
            this.resetState();
            return;
        }
        this.handleFileSelection(file);
    }

    handleFileSelection(file) {
        const {intl} = this.props;
        const extension = file.name.toLowerCase().split('.').pop();
        const allowedExtensions = ['csv', 'json'];

        if (!allowedExtensions.includes(extension)) {
            this.setState({
                fileName: file.name,
                format: null,
                parsedTextUnits: [],
                validationErrors: [],
                fileError: intl.formatMessage({id: "workbench.import.modal.error.invalidType"}),
                errorMessage: null,
                isDragging: false,
            });
            if (this.fileInputRef.current) {
                this.fileInputRef.current.value = '';
            }
            return;
        }

        const reader = new FileReader();
        reader.onload = (loadEvent) => {
            try {
                const result = this.parseImportedContent(file.name, loadEvent.target.result);
                this.setState({
                    fileName: file.name,
                    format: result.format,
                    parsedTextUnits: result.textUnits,
                    validationErrors: result.errors,
                    errorMessage: null,
                    fileError: null,
                    isDragging: false,
                });
            } catch (error) {
                this.setState({
                    fileName: file.name,
                    format: null,
                    parsedTextUnits: [],
                    validationErrors: [],
                    fileError: error.message || intl.formatMessage({id: "workbench.import.modal.error.unexpected"}),
                    errorMessage: null,
                    isDragging: false,
                });
            }
            if (this.fileInputRef.current) {
                this.fileInputRef.current.value = '';
            }
        };
        reader.onerror = () => {
            this.setState({
                fileName: file.name,
                format: null,
                parsedTextUnits: [],
                validationErrors: [],
                fileError: intl.formatMessage({id: "workbench.import.modal.error.read"}),
                errorMessage: null,
                isDragging: false,
            });
            if (this.fileInputRef.current) {
                this.fileInputRef.current.value = '';
            }
        };
        reader.readAsText(file);
    }

    parseImportedContent(fileName, content) {
        const trimmed = content ? content.trim() : '';
        if (!trimmed) {
            throw new Error(this.props.intl.formatMessage({id: "workbench.import.modal.error.empty"}));
        }

        const lowerName = fileName.toLowerCase();
        let format = null;
        if (lowerName.endsWith('.json')) {
            format = 'json';
        } else if (lowerName.endsWith('.csv')) {
            format = 'csv';
        } else if (trimmed.startsWith('[') || trimmed.startsWith('{')) {
            format = 'json';
        } else {
            format = 'csv';
        }

        const records = format === 'json' ? this.parseJson(trimmed) : this.parseCsv(trimmed);
        const {textUnits, errors} = this.normalizeRecords(records);
        return {format, textUnits, errors};
    }

    parseJson(content) {
        let parsed;
        try {
            parsed = JSON.parse(content);
        } catch (error) {
            throw new Error(this.props.intl.formatMessage({id: "workbench.import.modal.error.invalidJson"}));
        }

        if (Array.isArray(parsed)) {
            return parsed.map((record, index) => ({...record, __rowNumber: index + 1}));
        }

        if (parsed && Array.isArray(parsed.textUnits)) {
            return parsed.textUnits.map((record, index) => ({...record, __rowNumber: index + 1}));
        }

        throw new Error(this.props.intl.formatMessage({id: "workbench.import.modal.error.unexpectedJson"}));
    }

    parseCsv(content) {
        return csvToObjects(content);
    }

    normalizeRecords(records) {
        const {intl} = this.props;
        const errors = [];
        const textUnits = [];

        if (!Array.isArray(records)) {
            throw new Error(intl.formatMessage({id: "workbench.import.modal.error.invalidStructure"}));
        }

        records.forEach((rawRecord, index) => {
            const record = rawRecord || {};
            const rowNumber = record.__rowNumber || index + 1;

            const repositoryName = this.toNonEmptyString(record.repositoryName);
            if (!repositoryName) {
                errors.push(intl.formatMessage({id: "workbench.import.modal.error.missingRepository"}, {rowNumber}));
                return;
            }

            const assetPath = this.toNonEmptyString(record.assetPath);
            if (!assetPath) {
                errors.push(intl.formatMessage({id: "workbench.import.modal.error.missingAssetPath"}, {rowNumber}));
                return;
            }

            const targetLocale = this.toNonEmptyString(record.targetLocale || record.locale);
            if (!targetLocale) {
                errors.push(intl.formatMessage({id: "workbench.import.modal.error.missingTargetLocale"}, {rowNumber}));
                return;
            }

            const target = record.target;
            if (target === undefined || target === null) {
                errors.push(intl.formatMessage({id: "workbench.import.modal.error.missingTarget"}, {rowNumber}));
                return;
            }
            const normalizedTarget = String(target);

            const tmTextUnitId = this.parseInteger(record.tmTextUnitId);
            const name = this.toNonEmptyString(record.name);
            if (tmTextUnitId === null && !name) {
                errors.push(intl.formatMessage({id: "workbench.import.modal.error.missingIdentifier"}, {rowNumber}));
                return;
            }

            if (tmTextUnitId === undefined) {
                errors.push(intl.formatMessage({id: "workbench.import.modal.error.invalidTmTextUnitId"}, {rowNumber, value: record.tmTextUnitId}));
                return;
            }

            const textUnit = {
                repositoryName,
                assetPath,
                targetLocale,
                target: normalizedTarget,
            };

            if (tmTextUnitId !== null) {
                textUnit.tmTextUnitId = tmTextUnitId;
            }
            if (name) {
                textUnit.name = name;
            }

            const comment = this.toOptionalString(record.comment);
            if (comment !== undefined) {
                textUnit.comment = comment;
            }

            const targetComment = this.toOptionalString(record.targetComment);
            if (targetComment !== undefined) {
                textUnit.targetComment = targetComment;
            }

            const status = this.toOptionalString(record.status);
            if (status !== undefined) {
                textUnit.status = status.toUpperCase();
            }

            const includedRaw = record.includedInLocalizedFile;
            const includedInLocalizedFile = this.parseBoolean(includedRaw);
            if (includedInLocalizedFile !== undefined) {
                textUnit.includedInLocalizedFile = includedInLocalizedFile;
            } else if (this.isValueProvided(includedRaw)) {
                errors.push(intl.formatMessage({id: "workbench.import.modal.error.invalidIncludedInFile"}, {rowNumber, value: includedRaw}));
                return;
            }

            const doNotTranslateRaw = record.doNotTranslate;
            const doNotTranslate = this.parseBoolean(doNotTranslateRaw);
            if (doNotTranslate !== undefined) {
                textUnit.doNotTranslate = doNotTranslate;
            } else if (this.isValueProvided(doNotTranslateRaw)) {
                errors.push(intl.formatMessage({id: "workbench.import.modal.error.invalidDoNotTranslate"}, {rowNumber, value: doNotTranslateRaw}));
                return;
            }

            const pluralForm = this.toOptionalString(record.pluralForm);
            if (pluralForm !== undefined) {
                textUnit.pluralForm = pluralForm;
            }

            const pluralFormOther = this.toOptionalString(record.pluralFormOther);
            if (pluralFormOther !== undefined) {
                textUnit.pluralFormOther = pluralFormOther;
            }

            const branchId = this.parseInteger(record.branchId);
            if (branchId === undefined) {
                errors.push(intl.formatMessage({id: "workbench.import.modal.error.invalidBranchId"}, {rowNumber, value: record.branchId}));
                return;
            }
            if (branchId !== null) {
                textUnit.branchId = branchId;
            }

            const tmTextUnitVariantId = this.parseInteger(record.tmTextUnitVariantId);
            if (tmTextUnitVariantId === undefined) {
                errors.push(intl.formatMessage({id: "workbench.import.modal.error.invalidTmTextUnitVariantId"}, {rowNumber, value: record.tmTextUnitVariantId}));
                return;
            }
            if (tmTextUnitVariantId !== null) {
                textUnit.tmTextUnitVariantId = tmTextUnitVariantId;
            }

            const localeId = this.parseInteger(record.localeId);
            if (localeId === undefined) {
                errors.push(intl.formatMessage({id: "workbench.import.modal.error.invalidLocaleId"}, {rowNumber, value: record.localeId}));
                return;
            }
            if (localeId !== null) {
                textUnit.localeId = localeId;
            }

            const assetId = this.parseInteger(record.assetId);
            if (assetId === undefined) {
                errors.push(intl.formatMessage({id: "workbench.import.modal.error.invalidAssetId"}, {rowNumber, value: record.assetId}));
                return;
            }
            if (assetId !== null) {
                textUnit.assetId = assetId;
            }

            const createdDate = this.toOptionalString(record.createdDate);
            if (createdDate !== undefined) {
                textUnit.createdDate = createdDate;
            }

            const tmTextUnitCreatedDate = this.toOptionalString(record.tmTextUnitCreatedDate);
            if (tmTextUnitCreatedDate !== undefined) {
                textUnit.tmTextUnitCreatedDate = tmTextUnitCreatedDate;
            }

            textUnits.push(textUnit);
        });

        return {textUnits, errors};
    }

    toNonEmptyString(value) {
        if (value === undefined || value === null) {
            return null;
        }
        const asString = String(value).trim();
        return asString.length ? asString : null;
    }

    toOptionalString(value) {
        if (value === undefined || value === null) {
            return undefined;
        }
        const trimmed = String(value);
        return trimmed;
    }

    parseInteger(value) {
        if (value === undefined || value === null || value === '') {
            return null;
        }
        if (typeof value === 'number') {
            return Number.isFinite(value) ? value : undefined;
        }
        const parsed = parseInt(String(value).trim(), 10);
        if (isNaN(parsed)) {
            return undefined;
        }
        return parsed;
    }

    parseBoolean(value) {
        if (value === undefined || value === null || value === '') {
            return undefined;
        }
        if (typeof value === 'boolean') {
            return value;
        }
        const normalized = String(value).trim().toLowerCase();
        if (normalized === 'true' || normalized === '1') {
            return true;
        }
        if (normalized === 'false' || normalized === '0') {
            return false;
        }
        return undefined;
    }

    isValueProvided(value) {
        if (value === undefined || value === null) {
            return false;
        }
        if (typeof value === 'string') {
            return value.trim().length > 0;
        }
        return true;
    }

    isReadyForImport() {
        const {parsedTextUnits, validationErrors, fileError} = this.state;
        return parsedTextUnits.length > 0 && validationErrors.length === 0 && !fileError;
    }

    onDragOver(event) {
        event.preventDefault();
        if (this.state.isImporting) {
            return;
        }
        if (!this.state.isDragging) {
            this.setState({isDragging: true});
        }
    }

    onDragLeave(event) {
        event.preventDefault();
        if (this.state.isDragging) {
            this.setState({isDragging: false});
        }
    }

    onDrop(event) {
        event.preventDefault();
        if (this.state.isImporting) {
            return;
        }
        this.setState({isDragging: false});
        const files = event.dataTransfer && event.dataTransfer.files;
        if (files && files.length) {
            this.handleFileSelection(files[0]);
        }
    }

    onDropZoneClick() {
        if (this.state.isImporting) {
            return;
        }
        if (this.fileInputRef.current) {
            this.fileInputRef.current.click();
        }
    }

    onDropZoneKeyDown(event) {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            this.onDropZoneClick();
        }
    }

    downloadTemplate() {
        const headers = [
            'repositoryName',
            'assetPath',
            'targetLocale',
            'tmTextUnitId',
            'name',
            'target'
        ];
        const csv = `${headers.join(',')}\n`;
        const blob = new Blob([csv], {type: 'text/csv;charset=utf-8;'});
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'workbench-import-template.csv';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    }

    async startImport() {
        const {intl} = this.props;
        const {isImporting, parsedTextUnits} = this.state;

        if (isImporting || !this.isReadyForImport()) {
            return;
        }

        this.setState({isImporting: true, errorMessage: null});

        const payload = {
            textUnits: parsedTextUnits,
        };

        try {
            const pollableTask = await TextUnitClient.importTextUnitsBatch(payload);
            if (pollableTask && pollableTask.errorMessage) {
                throw new Error(pollableTask.errorMessage);
            }
            this.setState({isImporting: false}, () => {
                this.props.onClose();
            });
        } catch (error) {
            const message = error && error.message
                ? error.message
                : intl.formatMessage({id: "workbench.import.modal.error.generic"});
            this.setState({isImporting: false, errorMessage: message});
        }
    }

    renderValidationErrors() {
        if (!this.state.validationErrors.length) {
            return null;
        }
        return (
            <Alert bsStyle="warning">
                <FormattedMessage id="workbench.import.modal.error.listTitle"/>
                <ul className="mtm mbs">
                    {this.state.validationErrors.map((error, index) => <li key={index}>{error}</li>)}
                </ul>
            </Alert>
        );
    }

    renderSummary() {
        const {fileName, format, parsedTextUnits, validationErrors} = this.state;
        if (!fileName) {
            return null;
        }
        const normalizedFormat = format ? format.toUpperCase() : 'N/A';
        return (
            <Alert bsStyle="info">
                <FormattedMessage
                    id="workbench.import.modal.summary"
                    values={{
                        fileName,
                        format: normalizedFormat,
                        readyCount: parsedTextUnits.length,
                        skippedCount: validationErrors.length,
                    }}
                />
            </Alert>
        );
    }

    render() {
        const {fileName, isDragging, fileError, isImporting} = this.state;

        const dropzoneStyle = {
            border: `2px dashed ${fileError ? '#d9534f' : (isDragging ? '#5bc0de' : '#c0c0c0')}`,
            borderRadius: '4px',
            padding: '24px',
            textAlign: 'center',
            backgroundColor: isDragging ? '#f5fbff' : '#ffffff',
            cursor: isImporting ? 'not-allowed' : 'pointer',
            transition: 'border-color 0.2s ease, background-color 0.2s ease',
        };

        const dropzoneMessage = fileName ? (
            <span><FormattedMessage id="workbench.import.modal.dropSelected" values={{fileName}}/></span>
        ) : (
            <span>
                <strong><FormattedMessage id="workbench.import.modal.dropPrompt"/></strong>
                <br/>
                <FormattedMessage id="workbench.import.modal.dropHint"/>
            </span>
        );

        return (
            <Modal show={this.props.show}
                   onHide={() => !this.state.isImporting && this.props.onClose()}
                   dialogClassName="workbench-export-modal workbench-import-modal">
                <Modal.Header closeButton={!this.state.isImporting}>
                    <Modal.Title>
                        <FormattedMessage id="workbench.import.modal.title"/>
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p className="mbs">
                        <FormattedMessage id="workbench.import.modal.intro"/>
                    </p>
                    <ul className="list-unstyled mbs">
                        <li className="mvs">
                            <FormattedMessage id="workbench.import.modal.help.template"/>
                        </li>
                        <li className="mvs">
                            <FormattedMessage id="workbench.import.modal.help.required"/>
                        </li>
                    </ul>
                    <Button bsStyle="link"
                            onClick={() => this.downloadTemplate()}
                            disabled={isImporting}>
                        <FormattedMessage id="workbench.import.modal.templateLink"/>
                    </Button>

                    <FormGroup className="mtl">
                        <ControlLabel><FormattedMessage id="workbench.import.modal.selectFileLabel"/></ControlLabel>
                        <input
                            type="file"
                            accept=".csv, .json, text/csv, application/json"
                            ref={this.fileInputRef}
                            style={{display: 'none'}}
                            onChange={(event) => this.onFileChange(event)}
                            disabled={isImporting}
                        />
                        <div
                            style={dropzoneStyle}
                            className="workbench-import-dropzone"
                            onClick={() => this.onDropZoneClick()}
                            onDragOver={(event) => this.onDragOver(event)}
                            onDragEnter={(event) => this.onDragOver(event)}
                            onDragLeave={(event) => this.onDragLeave(event)}
                            onDrop={(event) => this.onDrop(event)}
                            role="button"
                            tabIndex={0}
                            aria-disabled={isImporting}
                            onKeyDown={(event) => this.onDropZoneKeyDown(event)}>
                            {dropzoneMessage}
                        </div>
                        <HelpBlock>
                            <FormattedMessage id="workbench.import.modal.selectFileHelp"/>
                        </HelpBlock>
                        {fileError && <div className="text-danger mtm">{fileError}</div>}
                    </FormGroup>

                    {this.renderSummary()}
                    {this.renderValidationErrors()}

                    {this.state.errorMessage &&
                        <Alert bsStyle="danger">{this.state.errorMessage}</Alert>
                    }
                </Modal.Body>
                <Modal.Footer>
                    {isImporting && <span className="glyphicon glyphicon-refresh spinning mrs"/>}
                    <Button onClick={() => this.props.onClose()} disabled={isImporting}>
                        <FormattedMessage id="label.cancel"/>
                    </Button>
                    <Button bsStyle="primary"
                            onClick={() => this.startImport()}
                            disabled={isImporting || !this.isReadyForImport()}>
                        <FormattedMessage id="workbench.import.modal.import"/>
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

export default injectIntl(ImportSearchResultsModal);
