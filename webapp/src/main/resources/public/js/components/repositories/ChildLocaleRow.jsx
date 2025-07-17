import React from "react";
import RepositoryLocaleDropdown from "./RepositoryLocaleDropdown";

const ChildLocaleRow = ({
    childLocale,
    childIndex,
    filteredChildOptions,
    onSelect,
    onRemove
}) => (
    <div className="repo-locale-row mll">
        <RepositoryLocaleDropdown
            className="repo-locale-dropdown"
            selectedLocale={childLocale.locale}
            localeOptions={filteredChildOptions}
            onSelect={selectedLocale => onSelect(childIndex, selectedLocale)}
        />
        <button
            type="button"
            className="btn btn-danger"
            onClick={() => onRemove(childIndex)}
        >
            Remove
        </button>
    </div>
);

export default ChildLocaleRow;
