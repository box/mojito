import React from "react";
import ThirdPartySyncActionsInput from "./ThirdPartySyncActionsInput";
import KeyValueInput from "./KeyValueInput";
import LabeledTextInput from "./LabeledTextInput";

const JobThirdPartyInput = ({
    thirdPartyProjectId,
    onInputChange,
    selectedActions,
    onActionsChange,
    localeMapping,
    onLocaleMappingChange
}) => (
    <div className="form-group">
        <LabeledTextInput
            label="Third Party Project ID*"
            placeholder="Enter Smartling Project Id"
            inputName="thirdPartyProjectId"
            value={thirdPartyProjectId}
            onChange={onInputChange}
        />
        <ThirdPartySyncActionsInput
            selectedActions={selectedActions}
            onChange={onActionsChange}
        />
        <KeyValueInput
            value={localeMapping}
            onChange={onLocaleMappingChange}
            inputLabel="Locale Mapping"
            keyLabel="Smartling Locale (en)"
            valueLabel="Mojito Locale (en-US)"
        />
    </div>
);

export default JobThirdPartyInput;
