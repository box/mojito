import React from "react";
import KeyValueInput from "./KeyValueInput";
import LabeledTextInput from "./LabeledTextInput";

const JobAdvancedInput = ({
    pluralSeparator,
    skipTextUnitsWithPattern,
    skipAssetsWithPathPattern,
    includeTextUnitsWithPattern,
    options,
    onInputChange,
    onOptionsMappingChange
}) => (
    <div className="form-group">
        <LabeledTextInput
            label="Plural Separator"
            placeholder="Enter plural separator"
            inputName="pluralSeparator"
            value={pluralSeparator}
            onChange={onInputChange}
        />
        <LabeledTextInput
            label="Skip Text Units With Pattern"
            placeholder="Enter skip text units pattern"
            inputName="skipTextUnitsWithPattern"
            value={skipTextUnitsWithPattern}
            onChange={onInputChange}
        />
        <LabeledTextInput
            label="Skip Assets With Path Pattern"
            placeholder="Enter skip assets with path pattern"
            inputName="skipAssetsWithPathPattern"
            value={skipAssetsWithPathPattern}
            onChange={onInputChange}
        />
        <LabeledTextInput
            label="Include Text Units With Pattern"
            placeholder="Enter include text units pattern"
            inputName="includeTextUnitsWithPattern"
            value={includeTextUnitsWithPattern}
            onChange={onInputChange}
        />
        <KeyValueInput
            value={options}
            onChange={onOptionsMappingChange}
            inputLabel="Options"
        />
    </div>
);

export default JobAdvancedInput;
