import React from "react";
import LabeledTextInput from "../jobs/LabeledTextInput";
import RepositoryLocaleDropdown from "./RepositoryLocaleDropdown";

const RepositoryGeneralInput = ({
    name,
    description,
    sourceLocale,
    checkSLA,
    assetIntegrityCheckers,
    hasValidIntegrityCheckers,
    onTextInputChange,
    onSourceLocaleChange,
    locales,
    onCheckSLAChange
}) => (
    <div className="form-group">
        <LabeledTextInput
            label="Repository Name*"
            placeholder="Enter repository name"
            inputName="name"
            value={name}
            onChange={onTextInputChange}
        />
        <LabeledTextInput
            label="Description"
            placeholder="Enter repository description"
            inputName="description"
            value={description}
            onChange={onTextInputChange}
        />
        <div className="mbm">
            <label>Source Locale*</label>
            <RepositoryLocaleDropdown
                localeOptions={locales || []}
                selectedLocale={sourceLocale} 
                onSelect={onSourceLocaleChange}
                defaultLocaleTag="en"
            />
        </div>
        <LabeledTextInput
            label="Asset Integrity Checkers"
            placeholder="Enter asset integrity checkers"
            inputName="assetIntegrityCheckers"
            value={assetIntegrityCheckers}
            onChange={onTextInputChange}
        />
        {!hasValidIntegrityCheckers && (
            <div className="alert alert-danger">
                Invalid asset integrity checkers format. Expects: "fileExtension: integrityChecker, ..."
            </div>
        )}
        <div className="form-check-control">
            <label className="form-check-label mrs">Check SLA</label>
            <input
                type="checkbox"
                className="form-check-input"
                checked={checkSLA}
                onChange={onCheckSLAChange}
            />
        </div>
    </div>
);

export default RepositoryGeneralInput;
