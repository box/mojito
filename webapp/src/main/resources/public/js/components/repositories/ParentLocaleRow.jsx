import React from "react";
import RepositoryLocaleDropdown from "./RepositoryLocaleDropdown";
import ChildLocaleRow from "./ChildLocaleRow";
import { OverlayTrigger, Tooltip } from "react-bootstrap";

const ParentLocaleRow = ({
    locale,
    index,
    parentLocales,
    locales,
    onParentSelect,
    onRemoveParent,
    onAddChild,
    onToggleFullyTranslated,
    onChildSelect,
    onRemoveChild
}) => {
    // For parent locale dropdown, returns options excluding its own children and already used parent tags
    const getParentLocaleDropdownOptions = ({ parentLocales, locales, locale, index }) => {
        const usedParentTags = new Set(
            parentLocales
                .filter((_, i) => i !== index)
                .map(l => l.locale.bcp47Tag)
        );
        const childTags = new Set(
            (locale.childLocales || [])
                .map(childLocale => childLocale.locale.bcp47Tag)
        );
        return locales.filter(option =>
            !usedParentTags.has(option.bcp47Tag) &&
            !childTags.has(option.bcp47Tag)
        );
    };

    // For child locale dropdown, returns options excluding its parent and all other children
    const getChildLocaleDropdownOptions = ({ locale, parentLocales, locales, index, childIndex }) => {
        const parentTag = locale.locale.bcp47Tag;
        const allChildTags = new Set(
            parentLocales.flatMap((parent, parentIndex) =>
                (parent.childLocales || [])
                    .filter((_, i) => !(parentIndex === index && i === childIndex))
                    .map(childLocale => childLocale.locale.bcp47Tag)
            )
        );
        return locales.filter(option =>
            option.bcp47Tag !== parentTag &&
            !allChildTags.has(option.bcp47Tag)
        );
    };

    return (
        <div>
            <div className="repo-locale-row">
                <RepositoryLocaleDropdown
                    className="repo-locale-dropdown"
                    selectedLocale={locale.locale}
                    localeOptions={getParentLocaleDropdownOptions({
                        parentLocales,
                        locales,
                        locale,
                        index
                    })}
                    onSelect={selectedLocale => onParentSelect(index, selectedLocale)}
                />
                <OverlayTrigger
                    placement="top"
                    overlay={<Tooltip id="ft-tooltip">To Be Fully Translated</Tooltip>}
                >
                    <input
                        type="checkbox"
                        checked={Boolean(locale.toBeFullyTranslated)}
                        onChange={e => onToggleFullyTranslated(index, e.target.checked)}
                        className="repo-locale-checkbox"
                    />
                </OverlayTrigger>
                <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => onAddChild(index)}
                >
                    Add Child
                </button>
                <button
                    type="button"
                    className="btn btn-danger"
                    onClick={() => onRemoveParent(index)}
                >
                    Remove
                </button>
            </div>
            {locale.childLocales && locale.childLocales.map((childLocale, childIndex) => (
                <ChildLocaleRow
                    key={childIndex}
                    childLocale={childLocale}
                    childIndex={childIndex}
                    filteredChildOptions={getChildLocaleDropdownOptions({
                        locale,
                        parentLocales,
                        locales,
                        index,
                        childIndex
                    })}
                    onSelect={(childIdx, selectedLocale) => onChildSelect(index, childIdx, selectedLocale)}
                    onRemove={childIdx => onRemoveChild(index, childIdx)}
                />
            ))}
        </div>
    );
};

export default ParentLocaleRow;
