import React from "react";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";
import ParentLocaleRow from "./ParentLocaleRow";

const EMPTY_PARENT_LOCALE = {
    locale: {bcp47Tag: "", id: null},
    toBeFullyTranslated: true,
    childLocales: []
};

const EMPTY_CHILD_LOCALE = {
    locale: {bcp47Tag: "", id: null},
    toBeFullyTranslated: false
};

const RepositoryLocalesInput = createReactClass({
    displayName: "RepositoryLocalesInput",
    propTypes: {
        locales: PropTypes.arrayOf(
            PropTypes.shape({
                bcp47Tag: PropTypes.string,
                id: PropTypes.number
            })
        ).isRequired,
        repositoryLocales: PropTypes.array.isRequired,
        onRepositoryLocalesChange: PropTypes.func.isRequired
    },

    handleAddParentLocale() {
        const newLocales = [...this.props.repositoryLocales, { ...EMPTY_PARENT_LOCALE }];
        this.props.onRepositoryLocalesChange(newLocales);
    },

    handleRemoveParentLocale(index) {
        const newLocales = [...this.props.repositoryLocales];
        newLocales.splice(index, 1);
        this.props.onRepositoryLocalesChange(newLocales);
    },

    handleParentLocaleSelect(index, selectedLocale) {
        const newLocales = [...this.props.repositoryLocales];
        newLocales[index] = { ...newLocales[index], locale: selectedLocale };
        this.props.onRepositoryLocalesChange(newLocales);
    },

    handleAddChildLocale(parentIndex) {
        const newLocales = [...this.props.repositoryLocales];
        const parent = { ...newLocales[parentIndex] };
        parent.childLocales = parent.childLocales ? [...parent.childLocales, { ...EMPTY_CHILD_LOCALE }] : [{ ...EMPTY_CHILD_LOCALE }];
        newLocales[parentIndex] = parent;
        this.props.onRepositoryLocalesChange(newLocales);
    },

    handleRemoveChildLocale(parentIndex, childIndex) {
        const newLocales = [...this.props.repositoryLocales];
        const parent = { ...newLocales[parentIndex] };
        const childLocales = [...(parent.childLocales || [])];
        childLocales.splice(childIndex, 1);
        parent.childLocales = childLocales;
        newLocales[parentIndex] = parent;
        this.props.onRepositoryLocalesChange(newLocales);
    },

    handleChildLocaleSelect(parentIndex, childIndex, selectedLocale) {
        const newLocales = [...this.props.repositoryLocales];
        const parent = { ...newLocales[parentIndex] };
        const childLocales = [...(parent.childLocales || [])];
        childLocales[childIndex] = { ...childLocales[childIndex], locale: selectedLocale };
        parent.childLocales = childLocales;
        newLocales[parentIndex] = parent;
        this.props.onRepositoryLocalesChange(newLocales);
    },

    handleToggleFullyTranslated(index, checked) {
        const newLocales = [...this.props.repositoryLocales];
        newLocales[index] = { ...newLocales[index], toBeFullyTranslated: checked };
        this.props.onRepositoryLocalesChange(newLocales);
    },

    render() {
        const parentLocales = this.props.repositoryLocales;
        return (
            <div>
                <label className="mbm">Repository Locales*</label>
                <div>
                    {parentLocales.length === 0 && (
                        <div className="mbm">
                            No locales added yet. Click "Add Locale" to start.
                        </div>
                    )}
                    {parentLocales.map((locale, index) => (
                        <ParentLocaleRow
                            key={index}
                            locale={locale}
                            index={index}
                            parentLocales={parentLocales}
                            locales={this.props.locales}
                            onParentSelect={this.handleParentLocaleSelect}
                            onRemoveParent={this.handleRemoveParentLocale}
                            onAddChild={this.handleAddChildLocale}
                            onToggleFullyTranslated={this.handleToggleFullyTranslated}
                            onChildSelect={this.handleChildLocaleSelect}
                            onRemoveChild={this.handleRemoveChildLocale}
                        />
                    ))}
                </div>
                <div className="mbm">
                    <button type="button" onClick={this.handleAddParentLocale} className="btn btn-default">Add Locale</button>
                </div>
            </div>
        );
    }
});

export default RepositoryLocalesInput;
