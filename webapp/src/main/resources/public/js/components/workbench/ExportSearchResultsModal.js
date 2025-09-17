import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Alert, Button, Checkbox, ControlLabel, FormControl, FormGroup, HelpBlock, Modal} from "react-bootstrap";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import {buildTextUnitSearcherParameters} from "../../utils/TextUnitSearcherParametersBuilder";
import TextUnitClient from "../../sdk/TextUnitClient";
import RepositoryStore from "../../stores/RepositoryStore";
import {buildZipFile} from "../../utils/ZipBuilder";

const DEFAULT_LIMIT = 10000;
const DEFAULT_FIELDS = [
    "tmTextUnitId",
    "repositoryName",
    "used",
    "targetLocale",
    "source",
    "target",
    "comment",
    "assetPath"
];
const MAX_BATCH_SIZE = 1000;
const EXPORT_FORMATS = {
    "JSON": "json",
    "CSV": "csv"
};

const EXPORTABLE_FIELDS = [
    {name: "assetId", labelId: "workbench.export.fields.assetId"},
    {name: "assetPath", labelId: "workbench.export.fields.assetPath"},
    {name: "branchId", labelId: "workbench.export.fields.branchId"},
    {name: "comment", labelId: "workbench.export.fields.comment"},
    {name: "createdDate", labelId: "workbench.export.fields.createdDate"},
    {name: "doNotTranslate", labelId: "workbench.export.fields.doNotTranslate"},
    {name: "includedInLocalizedFile", labelId: "workbench.export.fields.includedInLocalizedFile"},
    {name: "localeId", labelId: "workbench.export.fields.localeId"},
    {name: "name", labelId: "workbench.export.fields.name"},
    {name: "pluralForm", labelId: "workbench.export.fields.pluralForm"},
    {name: "pluralFormOther", labelId: "workbench.export.fields.pluralFormOther"},
    {name: "repositoryName", labelId: "workbench.export.fields.repositoryName"},
    {name: "source", labelId: "workbench.export.fields.source"},
    {name: "status", labelId: "workbench.export.fields.status"},
    {name: "target", labelId: "workbench.export.fields.target"},
    {name: "targetComment", labelId: "workbench.export.fields.targetComment"},
    {name: "targetLocale", labelId: "workbench.export.fields.targetLocale"},
    {name: "tmTextUnitCreatedDate", labelId: "workbench.export.fields.tmTextUnitCreatedDate"},
    {name: "tmTextUnitId", labelId: "workbench.export.fields.tmTextUnitId"},
    {name: "tmTextUnitVariantId", labelId: "workbench.export.fields.tmTextUnitVariantId"},
    {name: "used", labelId: "workbench.export.fields.used"},
];

const EXPORT_FIELD_PRIORITY = [
    "targetLocale",
    "source",
    "comment",
    "target",
    "repositoryName",
    "tmTextUnitId",
];

const FIELD_EXTRACTORS = {
    "tmTextUnitId": (textUnit) => textUnit.getTmTextUnitId(),
    "tmTextUnitVariantId": (textUnit) => textUnit.getTmTextUnitVariantId(),
    "localeId": (textUnit) => textUnit.getLocaleId(),
    "targetLocale": (textUnit) => textUnit.getTargetLocale(),
    "repositoryName": (textUnit) => textUnit.getRepositoryName(),
    "assetId": (textUnit) => textUnit.getAssetId(),
    "assetPath": (textUnit) => textUnit.getAssetPath(),
    "name": (textUnit) => textUnit.getName(),
    "source": (textUnit) => textUnit.getSource(),
    "target": (textUnit) => textUnit.getTarget(),
    "comment": (textUnit) => textUnit.getComment(),
    "targetComment": (textUnit) => textUnit.getTargetComment(),
    "status": (textUnit) => textUnit.getStatus(),
    "includedInLocalizedFile": (textUnit) => textUnit.isIncludedInLocalizedFile(),
    "doNotTranslate": (textUnit) => textUnit.getDoNotTranslate(),
    "createdDate": (textUnit) => textUnit.getCreatedDate(),
    "tmTextUnitCreatedDate": (textUnit) => textUnit.getTmTextUnitCreatedDate(),
    "pluralForm": (textUnit) => textUnit.getPluralForm(),
    "pluralFormOther": (textUnit) => textUnit.getPluralFormOther(),
    "branchId": (textUnit) => textUnit.data && textUnit.data.branchId,
    "used": (textUnit) => textUnit.isUsed()
};

class ExportSearchResultsModal extends React.Component {
    static propTypes = {
        "show": PropTypes.bool.isRequired,
        "onClose": PropTypes.func.isRequired,
    };

    constructor(props) {
        super(props);
        this.state = this.getDefaultState();
    }

    componentDidUpdate(prevProps) {
        if (!prevProps.show && this.props.show) {
            this.setState(this.getDefaultState());
        }
    }

    getDefaultState() {
        const {availableLocales, defaultSelectedLocales} = this.getInitialLocales();
        return {
            selectedFields: DEFAULT_FIELDS.slice(),
            limit: DEFAULT_LIMIT.toString(),
            isExporting: false,
            errorMessage: null,
            format: EXPORT_FORMATS.CSV,
            splitByLocale: false,
            availableLocales,
            selectedLocales: defaultSelectedLocales,
        };
    }

    getInitialLocales() {
        const searchParams = {...SearchParamsStore.getState()};
        const repoIds = Array.isArray(searchParams.repoIds) ? searchParams.repoIds : [];
        let availableLocales = [];
        if (repoIds && repoIds.length) {
            availableLocales = RepositoryStore.getAllBcp47TagsForRepositoryIds(repoIds) || [];
        }
        availableLocales = Array.from(new Set(availableLocales)).sort();

        let defaultSelectedLocales = [];
        if (Array.isArray(searchParams.bcp47Tags) && searchParams.bcp47Tags.length) {
            defaultSelectedLocales = searchParams.bcp47Tags.filter(tag => availableLocales.indexOf(tag) !== -1);
        } else {
            defaultSelectedLocales = availableLocales.slice();
        }

        return {availableLocales, defaultSelectedLocales};
    }

    toggleField(fieldName) {
        this.setState((prevState) => {
            let nextFields;
            if (prevState.selectedFields.indexOf(fieldName) !== -1) {
                nextFields = prevState.selectedFields.filter(field => field !== fieldName);
            } else {
                nextFields = prevState.selectedFields.concat(fieldName);
            }
            return {selectedFields: nextFields};
        });
    }

    onLimitChange(event) {
        this.setState({limit: event.target.value});
    }

    onFormatChange(event) {
        this.setState({format: event.target.value});
    }

    async startExport() {
        const {intl} = this.props;
        const {selectedFields, format} = this.state;
        const parsedLimit = parseInt(this.state.limit, 10);

        if (!selectedFields.length) {
            this.setState({errorMessage: intl.formatMessage({id: "workbench.export.modal.error.fieldsRequired"})});
            return;
        }

        if (isNaN(parsedLimit) || parsedLimit <= 0) {
            this.setState({errorMessage: intl.formatMessage({id: "workbench.export.modal.error.limitInvalid"})});
            return;
        }

        const baseSearchParams = {...SearchParamsStore.getState()};
        const fieldsForExport = EXPORT_FIELD_PRIORITY
            .filter(field => selectedFields.indexOf(field) !== -1)
            .concat(selectedFields.filter(field => EXPORT_FIELD_PRIORITY.indexOf(field) === -1));

        if (this.state.splitByLocale && this.state.selectedLocales.length) {
            this.setState({isExporting: true, errorMessage: null});
            try {
                await this.exportPerLocaleWithFetch(parsedLimit, fieldsForExport, format, baseSearchParams);
            } catch (error) {
                console.error("Failed to export search results", error);
                const fallbackMessage = intl.formatMessage({id: "workbench.export.modal.error.generic"});
                const normalizedError = (error && error.message) ? error.message : null;
                this.setState({
                    isExporting: false,
                    errorMessage: normalizedError ? `${fallbackMessage} (${normalizedError})` : fallbackMessage,
                });
            }
            return;
        }

        const {textUnitSearcherParameters, returnEmpty} = buildTextUnitSearcherParameters(baseSearchParams);

        if (returnEmpty) {
            this.setState({errorMessage: intl.formatMessage({id: "workbench.export.modal.error.searchNotReady"})});
            return;
        }

        textUnitSearcherParameters.offset(0);

        this.setState({isExporting: true, errorMessage: null});

        try {
            const textUnits = await this.fetchAllTextUnits(textUnitSearcherParameters, parsedLimit);
            const rows = this.buildRows(textUnits, fieldsForExport);
            const {blob, extension} = this.buildExportPayload(rows, fieldsForExport, format);
            this.triggerDownload(blob, `workbench-export-${Date.now()}.${extension}`);
            this.setState({isExporting: false}, () => this.props.onClose());
        } catch (error) {
            console.error("Failed to export search results", error);
            const fallbackMessage = intl.formatMessage({id: "workbench.export.modal.error.generic"});
            const normalizedError = (error && error.message) ? error.message : null;
            this.setState({
                isExporting: false,
                errorMessage: normalizedError ? `${fallbackMessage} (${normalizedError})` : fallbackMessage,
            });
        }
    }

    isExportDisabled() {
        if (this.state.isExporting) {
            return true;
        }
        if (!this.state.selectedFields.length) {
            return true;
        }
        if (this.state.splitByLocale && !this.state.selectedLocales.length) {
            return true;
        }
        return false;
    }

    onSplitByLocaleToggle(splitByLocale) {
        if (this.state.isExporting) {
            return;
        }
        if (splitByLocale && !this.state.availableLocales.length) {
            this.setState({splitByLocale: false});
            return;
        }
        let nextSelectedLocales = this.state.selectedLocales;
        if (splitByLocale && nextSelectedLocales.length === 0) {
            nextSelectedLocales = this.state.availableLocales.slice();
        }
        this.setState({splitByLocale, selectedLocales: nextSelectedLocales});
    }

    onLocalesChange(event) {
        if (this.state.isExporting) {
            return;
        }
        const options = event.target.options;
        const selectedLocales = [];
        for (let i = 0; i < options.length; i++) {
            if (options[i].selected) {
                selectedLocales.push(options[i].value);
            }
        }
        this.setState({selectedLocales});
    }

    buildExportPayload(rows, fieldsForExport, format) {
        const encoder = new TextEncoder();
        if (format === EXPORT_FORMATS.CSV) {
            const csv = this.convertRowsToCsv(rows, fieldsForExport);
            const bytes = encoder.encode(csv);
            return {
                blob: new Blob([bytes], {type: "text/csv;charset=utf-8;"}),
                extension: "csv",
                bytes,
            };
        }
        const json = JSON.stringify(rows, null, 2);
        const bytes = encoder.encode(json);
        return {
            blob: new Blob([bytes], {type: "application/json;charset=utf-8;"}),
            extension: "json",
            bytes,
        };
    }

    async exportPerLocaleWithFetch(limit, fieldsForExport, format, baseSearchParams) {
        const {intl} = this.props;
        const {selectedLocales} = this.state;
        const timestamp = Date.now();
        let filesGenerated = 0;
        const missingLocales = [];
        const files = [];

        for (const locale of selectedLocales) {
            const localeSearchParams = {...baseSearchParams, bcp47Tags: [locale]};
            const {textUnitSearcherParameters, returnEmpty} = buildTextUnitSearcherParameters(localeSearchParams);

            if (returnEmpty) {
                missingLocales.push(locale);
                continue;
            }

            textUnitSearcherParameters.offset(0);

            const localeTextUnits = await this.fetchAllTextUnits(textUnitSearcherParameters, limit);

            if (!localeTextUnits.length) {
                missingLocales.push(locale);
                continue;
            }

            const rows = this.buildRows(localeTextUnits, fieldsForExport);
            const payload = this.buildExportPayload(rows, fieldsForExport, format);
            const safeLocale = locale.replace(/[^A-Za-z0-9._-]/g, '_');
            files.push({
                name: `workbench-export-${safeLocale}-${timestamp}.${payload.extension}`,
                content: payload.bytes,
            });
            filesGenerated += 1;
        }

        if (!filesGenerated) {
            const message = selectedLocales.length
                ? intl.formatMessage({id: "workbench.export.modal.error.localesEmpty"})
                : intl.formatMessage({id: "workbench.export.modal.error.localesMissingSelection"});
            this.setState({isExporting: false, errorMessage: message});
            return;
        }

        const zipArray = buildZipFile(files);
        const zipBlob = new Blob([zipArray], {type: "application/zip"});
        this.triggerDownload(zipBlob, `workbench-export-locales-${timestamp}.zip`);

        if (missingLocales.length) {
            const message = intl.formatMessage(
                {id: "workbench.export.modal.error.localesSkipped"},
                {locales: missingLocales.join(', ')}
            );
            this.setState({isExporting: false, errorMessage: message});
            return;
        }

        this.setState({isExporting: false}, () => this.props.onClose());
    }

    renderFields() {
        const {intl} = this.props;
        const containerStyle = {
            display: 'flex',
            flexWrap: 'wrap',
            marginLeft: '-12px',
            marginRight: '-12px',
        };
        const fieldStyle = {
            display: 'inline-flex',
            alignItems: 'center',
            flex: '0 0 33%',
            maxWidth: '33%',
            paddingLeft: '12px',
            paddingRight: '12px',
            marginBottom: '12px',
            boxSizing: 'border-box',
            fontWeight: 'normal',
        };
        const checkboxStyle = {
            marginRight: '6px',
        };
        const fieldOptions = EXPORTABLE_FIELDS
            .map(field => ({
                ...field,
                label: intl.formatMessage({id: field.labelId}),
            }))
            .sort((fieldA, fieldB) => fieldA.label.localeCompare(fieldB.label));
        return (
            <div className="workbench-export-fields" style={containerStyle}>
                {fieldOptions.map(field => (
                    <label key={field.name}
                           className="workbench-export-field-option"
                           style={fieldStyle}>
                        <input
                            type="checkbox"
                            checked={this.state.selectedFields.indexOf(field.name) !== -1}
                            onChange={() => this.toggleField(field.name)}
                            style={checkboxStyle}
                        />
                        <span>{field.label}</span>
                    </label>
                ))}
            </div>
        );
    }

    render() {
        const {intl} = this.props;
        const {splitByLocale, availableLocales, selectedLocales} = this.state;
        const localesEnabled = availableLocales.length > 0;
        return (
            <Modal show={this.props.show}
                   onHide={() => !this.state.isExporting && this.props.onClose()}
                   dialogClassName="workbench-export-modal">
                <Modal.Header closeButton={!this.state.isExporting}>
                    <Modal.Title>
                        <FormattedMessage id="workbench.export.modal.title"/>
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p>
                        <FormattedMessage id="workbench.export.modal.description"/>
                    </p>
                    <FormGroup>
                        <ControlLabel><FormattedMessage id="workbench.export.modal.formatLabel"/></ControlLabel>
                        <FormControl componentClass="select"
                                     value={this.state.format}
                                     onChange={(e) => this.onFormatChange(e)}
                                     disabled={this.state.isExporting}>
                            <option value={EXPORT_FORMATS.JSON}>{intl.formatMessage({id: "workbench.export.modal.format.json"})}</option>
                            <option value={EXPORT_FORMATS.CSV}>{intl.formatMessage({id: "workbench.export.modal.format.csv"})}</option>
                        </FormControl>
                        <Checkbox
                            className="mtm"
                            checked={splitByLocale}
                            onChange={(e) => this.onSplitByLocaleToggle(e.target.checked)}
                            disabled={this.state.isExporting || !localesEnabled}>
                            <FormattedMessage id="workbench.export.modal.splitByLocale"/>
                        </Checkbox>
                        {!localesEnabled &&
                            <HelpBlock><FormattedMessage id="workbench.export.modal.splitByLocale.disabled"/></HelpBlock>
                        }
                        {splitByLocale && localesEnabled &&
                            <div className="mtm">
                                <ControlLabel><FormattedMessage id="workbench.export.modal.localesLabel"/></ControlLabel>
                                <FormControl componentClass="select"
                                             multiple
                                             value={selectedLocales}
                                             onChange={(e) => this.onLocalesChange(e)}
                                             disabled={this.state.isExporting}>
                                    {availableLocales.map(locale => (
                                        <option key={locale} value={locale}>{locale}</option>
                                    ))}
                                </FormControl>
                                <HelpBlock><FormattedMessage id="workbench.export.modal.localesHelp"/></HelpBlock>
                            </div>
                        }
                    </FormGroup>
                    <FormGroup>
                        <ControlLabel><FormattedMessage id="workbench.export.modal.fieldsLabel"/></ControlLabel>
                        {this.renderFields()}
                    </FormGroup>
                    <FormGroup>
                        <ControlLabel><FormattedMessage id="workbench.export.modal.limitLabel"/></ControlLabel>
                        <FormControl
                            type="number"
                            min="1"
                            value={this.state.limit}
                            onChange={(e) => this.onLimitChange(e)}
                            disabled={this.state.isExporting}
                        />
                    </FormGroup>
                    {this.state.errorMessage &&
                        <Alert bsStyle="danger">{this.state.errorMessage}</Alert>
                    }
                </Modal.Body>
                <Modal.Footer>
                    {this.state.isExporting && <span className="glyphicon glyphicon-refresh spinning mrs"/>}
                    <Button onClick={() => this.props.onClose()} disabled={this.state.isExporting}>
                        <FormattedMessage id="label.cancel"/>
                    </Button>
                    <Button bsStyle="primary"
                            onClick={() => this.startExport()}
                            disabled={this.isExportDisabled()}>
                        <FormattedMessage id="workbench.export.modal.export"/>
                    </Button>
                </Modal.Footer>
            </Modal>
        );
}

    async fetchAllTextUnits(textUnitSearcherParameters, limit) {
        const results = [];
        let offset = 0;

        while (results.length < limit) {
            const remaining = limit - results.length;
            const batchSize = Math.min(remaining, MAX_BATCH_SIZE);
            textUnitSearcherParameters.limit(batchSize);
            textUnitSearcherParameters.offset(offset);

            const batch = await TextUnitClient.getTextUnits(textUnitSearcherParameters);

            if (!batch.length) {
                break;
            }

            results.push(...batch);

            if (batch.length < batchSize) {
                break;
            }

            offset += batchSize;
        }

        return results.slice(0, limit);
    }

    buildRows(textUnits, fields) {
        return textUnits.map(textUnit => {
            const row = {};
            fields.forEach(field => {
                const extractor = FIELD_EXTRACTORS[field];
                const value = extractor ? extractor(textUnit) : (textUnit.data ? textUnit.data[field] : null);
                row[field] = (typeof value === "undefined") ? null : value;
            });
            return row;
        });
    }

    convertRowsToCsv(rows, fields) {
        const header = fields.join(",");
        const lines = rows.map(row => fields.map(field => this.escapeCsvValue(row[field])).join(","));
        return [header].concat(lines).join("\n");
    }

    escapeCsvValue(value) {
        if (value === null || typeof value === "undefined") {
            return "";
        }

        const stringValue = String(value);
        const mustQuote = stringValue.indexOf(',') !== -1 || stringValue.indexOf('\n') !== -1 || stringValue.indexOf('"') !== -1;
        let escaped = stringValue.replace(/"/g, '""');
        if (mustQuote) {
            escaped = `"${escaped}"`;
        }
        return escaped;
    }

    triggerDownload(blob, fileName) {
        const blobUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = blobUrl;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(blobUrl);
    }
}

export default injectIntl(ExportSearchResultsModal);
