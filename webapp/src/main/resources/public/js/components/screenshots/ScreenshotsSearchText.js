import keycode from "keycode";
import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {DropdownButton, FormGroup, FormControl, InputGroup, MenuItem, Button, Glyphicon} from "react-bootstrap";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchConstants from "../../utils/SearchConstants";
import ScreenshotsSearchTextStore from "../../stores/screenshots/ScreenshotsSearchTextStore";

class ScreenshotsSearchText extends React.Component {
    
    static propTypes = {
        "searchAttribute": PropTypes.string.isRequired,
        "searchType": PropTypes.string.isRequired,
        "searchText": PropTypes.string.isRequired,
        "isSpinnerShown": PropTypes.bool.isRequired,
        
        "onSearchAttributeChanged": PropTypes.func.isRequired,
        "onSearchTypeChanged": PropTypes.func.isRequired,
        "onSearchTextChanged": PropTypes.func.isRequired,
        "onPerformSearch": PropTypes.func.isRequired
    }

    onSearchAttributeSelected(searchAttribute) {
        if (searchAttribute !== this.props.searchAttribute) {
            this.props.onSearchAttributeChanged(searchAttribute);
        }
    }

    onSearchTypeSelected(searchType) {
        if (searchType !== this.props.searchType) {
            this.props.onSearchTypeChanged(searchType); 
        }
    }

    onKeyDownOnSearchText(e) {
        e.stopPropagation();
        if (e.keyCode === keycode("enter")) {
            this.props.onPerformSearch();
        }
    }

    onSearchButtonClicked() {
        this.props.onPerformSearch();
    }

    getMessageForSearchAttribute(searchAttribute) {
        switch (searchAttribute) {
            case SearchParamsStore.SEARCH_ATTRIBUTES.STRING_ID:
                return this.props.intl.formatMessage({id: "search.filter.id"});
            case SearchParamsStore.SEARCH_ATTRIBUTES.SOURCE:
                return this.props.intl.formatMessage({id: "search.filter.source"});
            case SearchParamsStore.SEARCH_ATTRIBUTES.TARGET:
                return this.props.intl.formatMessage({id: "search.filter.target"});
            case ScreenshotsSearchTextStore.SEARCH_ATTRIBUTES_SCREENSHOT:
                return this.props.intl.formatMessage({id: "search.filter.screenshot"});
        }
    }

    getMessageForSearchType(searchType) {
        switch (searchType) {
            case SearchParamsStore.SEARCH_TYPES.EXACT:
                return this.props.intl.formatMessage({id: "search.filter.exact"});
            case SearchParamsStore.SEARCH_TYPES.CONTAINS:
                return this.props.intl.formatMessage({id: "search.filter.contains"});
            case SearchParamsStore.SEARCH_TYPES.ILIKE:
                return this.props.intl.formatMessage({id: "search.filter.ilike"});
        }
    }

    renderSearchAttributeMenuItem(searchAttribute) {
        return (
            <MenuItem eventKey={searchAttribute} active={this.props.searchAttribute === searchAttribute}
                      onSelect={(e) => this.onSearchAttributeSelected(e)}>
                {this.getMessageForSearchAttribute(searchAttribute)}
            </MenuItem>
        );
    }

    renderSearchTypeMenuItem(searchType) {
        return (
            <MenuItem eventKey={searchType} active={this.props.searchType === searchType}
                      onSelect={(e) => this.onSearchTypeSelected(e)}>
                {this.getMessageForSearchType(searchType)}
            </MenuItem>
        );
    }

    renderDropdown() {
        
        return (
            <DropdownButton id="search-attribute-dropdown" title={this.getMessageForSearchAttribute(this.props.searchAttribute)}>
                <MenuItem header><FormattedMessage id="search.filter.searchAttribute"/></MenuItem>
                {this.renderSearchAttributeMenuItem(SearchParamsStore.SEARCH_ATTRIBUTES.STRING_ID)}
                {this.renderSearchAttributeMenuItem(SearchParamsStore.SEARCH_ATTRIBUTES.SOURCE)}
                {this.renderSearchAttributeMenuItem(SearchParamsStore.SEARCH_ATTRIBUTES.TARGET)}
                {this.renderSearchAttributeMenuItem(ScreenshotsSearchTextStore.SEARCH_ATTRIBUTES_SCREENSHOT)}
                <MenuItem divider/>
                <MenuItem header><FormattedMessage id="search.filter.searchType"/></MenuItem>
                {this.renderSearchTypeMenuItem(SearchParamsStore.SEARCH_TYPES.EXACT)}
                {this.renderSearchTypeMenuItem(SearchParamsStore.SEARCH_TYPES.CONTAINS)}
                {this.renderSearchTypeMenuItem(SearchParamsStore.SEARCH_TYPES.ILIKE)}
            </DropdownButton>
        );
    };

    renderSearchButton() {
        return (
            <Button onClick={() => this.onSearchButtonClicked()}>
                <Glyphicon glyph='glyphicon glyphicon-search'/>
            </Button>
        );
    };

    render() {
        return (
            <div className="col-xs-6 search-text">
                <FormGroup>
                    <InputGroup>
                        <InputGroup.Button>{this.renderDropdown()}</InputGroup.Button>
                        <FormControl type='text' value={this.props.searchText}
                                     onChange={(e) => this.props.onSearchTextChanged(e.target.value)}
                                     placeholder={this.props.intl.formatMessage({ id: "search.placeholder" })}
                                     onKeyDown={(e) => this.onKeyDownOnSearchText(e)}/>
                        <InputGroup>
                            {this.props.isSpinnerShown && (<span className="glyphicon glyphicon-refresh spinning" />)}
                        </InputGroup>
                        <InputGroup.Button>{this.renderSearchButton()}</InputGroup.Button>
                    </InputGroup>
                </FormGroup>
            </div>
        );
    }
};

export default injectIntl(ScreenshotsSearchText);
