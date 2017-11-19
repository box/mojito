import $ from "jquery";
import _ from "lodash";
import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from 'react-intl';
import {DropdownButton, MenuItem} from "react-bootstrap";
import Locales from "../../utils/Locales";

class LocalesDropDown extends React.Component {
    
    static propTypes = {
        "bcp47Tags": PropTypes.array.isRequired,
        "fullyTranslatedBcp47Tags": PropTypes.array.isRequired,
        "selectedBcp47Tags": PropTypes.array.isRequired,
        "onSelectedBcp47TagsChanged": PropTypes.func.isRequired,
        "onDropdownToggle": PropTypes.func.isRequired,
        "dropdownOpen": PropTypes.bool.isRequired
    }

    /**
     * Get list of locales (with selected state) sorted by their display name
     *
     * @return {{bcp47Tag: string, displayName: string, selected: boolean}[]}}
     */
    getSortedLocales() {
        let locales = this.props.bcp47Tags
                .map((bcp47Tag) => {
                    return {
                        "bcp47Tag": bcp47Tag,
                        "displayName": Locales.getDisplayName(bcp47Tag),
                        "selected": this.props.selectedBcp47Tags.indexOf(bcp47Tag) > -1
                    }
                }).sort((a, b) => a.displayName.localeCompare(b.displayName));

        return locales;
    }

    /**
     * On dropdown selected event, add or remove the target locale from the
     * selected locale list base on its previous state (selected or not).
     *
     * @param locale the locale that was selected
     */
    onLocaleSelected(locale) {
        
        let bcp47Tag = locale.bcp47Tag;

        let newSelectedBcp47Tags = this.props.selectedBcp47Tags.slice();

        if (locale.selected) {
            _.pull(newSelectedBcp47Tags, bcp47Tag);
        } else {
            newSelectedBcp47Tags.push(bcp47Tag);
        }

        this.callOnSelectedBcp47TagsChanged(newSelectedBcp47Tags);
    }

    /**
     * Trigger the searchParamsChanged action for a given list of selected
     * bcp47 tags.
     *
     * @param newSelectedBcp47Tags
     */
    callOnSelectedBcp47TagsChanged(newSelectedBcp47Tags) {
        this.props.onSelectedBcp47TagsChanged(newSelectedBcp47Tags);
    }

    /**
     * Gets the text to display on the button.
     *
     * if 1 locale selected the named is shown, else the number of selected locale is displayed (with proper i18n support)
     *
     * @returns {string} text to display on the button
     */
    getButtonText() {

        let label = '';

        let numberOfSelectedLocales = this.props.selectedBcp47Tags.length;

        if (numberOfSelectedLocales === 1) {
            label = Locales.getDisplayName(this.props.selectedBcp47Tags[0]);
        } else {
            label = this.props.intl.formatMessage({id: "search.locale.btn.text"}, {'numberOfSelectedLocales': numberOfSelectedLocales});
        }

        return label;
    }

    onDropdownToggle(newOpenState, event, source) {
        if (source && source.source !== 'select') {
            this.props.onDropdownToggle(newOpenState);
        }
    }

    /**
     * Selects fully translated locales.
     */
    onSelectToBeFullyTranslated() {
        this.callOnSelectedBcp47TagsChanged(this.props.fullyTranslatedBcp47Tags.slice());
    }

    /**
     * Selects all locales.
     */
    onSelectAll() {
        this.callOnSelectedBcp47TagsChanged(this.props.bcp47Tags.slice());
    }

    /**
     * Clear all selected locales.
     */
    onSelectNone() {
        this.callOnSelectedBcp47TagsChanged([]);
    }

    /**
     * Indicates if the select to be fully translated menu item should be active.
     *
     * @returns {boolean}
     */
    isToBeFullyTranslatedActive() {
        return this.props.selectedBcp47Tags.length > 0 && _.isEqual(this.props.selectedBcp47Tags, this.props.fullyTranslatedBcp47Tags);
    }

    /**
     * Indicates if the select all menu item should be active.
     *
     * @returns {boolean}
     */
    isAllActive() {
        return this.props.selectedBcp47Tags.length > 0 && this.props.selectedBcp47Tags.length === this.props.bcp47Tags.length;
    }

    /**
     * Indicates if the clear all menu item should be active.
     *
     * @returns {boolean}
     */
    isNoneActive() {
        return this.props.selectedBcp47Tags.length === 0;
    }

    /**
     * Renders the locale menu item list.
     *
     * @returns {XML}
     */
    renderLocales() {
        return this.getSortedLocales().map(
                (locale) =>
                        <MenuItem key={locale.displayName} eventKey={locale} active={locale.selected} onSelect={(locale) => this.onLocaleSelected(locale)}>{locale.displayName}</MenuItem>
        );
    }

    /**
     * @return {JSX}
     */
    render() {
        return (
                <span className="mlm locale-dropdown">
                <DropdownButton 
                        id="localesDropdown" 
                        title={this.getButtonText()} 
                        onToggle={(newOpenState, event, source) => this.onDropdownToggle(newOpenState, event, source)} 
                        open={this.props.dropdownOpen}
                        disabled={this.props.bcp47Tags.length === 0}>
                    <MenuItem key="1" active={this.isToBeFullyTranslatedActive()} onSelect={() => this.onSelectToBeFullyTranslated()}><FormattedMessage id="search.locale.selectToBeFullyTranslated"/></MenuItem>
                    <MenuItem key="2" active={this.isAllActive()} onSelect={() => this.onSelectAll()}><FormattedMessage id="search.locale.selectAll"/></MenuItem>
                    <MenuItem key="3" active={this.isNoneActive()} onSelect={() => this.onSelectNone()}><FormattedMessage id="search.locale.selectNone"/></MenuItem>
                    <MenuItem divider/>
                    {this.renderLocales()}
                </DropdownButton>
                </span>
        );

    }
}

export default injectIntl(LocalesDropDown);
