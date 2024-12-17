import _ from "lodash";
import React from "react";
import createReactClass from 'create-react-class';
import {FormattedMessage, injectIntl} from 'react-intl';
import {DropdownButton, MenuItem, InputGroup, FormControl, Button, Glyphicon} from "react-bootstrap";
import DateTime from "react-datetime";
import FluxyMixin from "alt-mixins/FluxyMixin";
import moment from 'moment';
import keycode from 'keycode';

import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchConstants from "../../utils/SearchConstants";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";

let StatusDropdown = createReactClass({
    displayName: 'StatusDropdown',
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onSearchParamsChanged": SearchParamsStore
        }
    },

    onSearchParamsChanged() {

        var searchParams = SearchParamsStore.getState();

        this.setState({
            "status": searchParams.status,
            "used": searchParams.used,
            "unUsed": searchParams.unUsed,
            "translate" : searchParams.translate,
            "doNotTranslate" : searchParams.doNotTranslate,
            "tmTextUnitCreatedBefore" : searchParams.tmTextUnitCreatedBefore,
            "tmTextUnitCreatedAfter" : searchParams.tmTextUnitCreatedAfter
        });
    },

    getInitialState() {
        return {
            "status": this.getInitialStatus(),
            "used": false,
            "unUsed": false,
            "translate" : false,
            "doNotTranslate" : false,
            "tmTextUnitCreatedBefore" : null,
            "tmTextUnitCreatedBeforeTyping": null,
            "tmTextUnitCreatedAfter" : null,
            "tmTextUnitCreatedAfterTyping": null,
        };
    },

    /**
     * Get initial searchAttribute value
     *
     * @return {string}
     */
    getInitialStatus() {
        return this.props.status ? this.props.status : SearchParamsStore.STATUS.ALL;
    },

    onStatusSelected(status) {
        if (status !== this.state.status) {
            this.setStateAndCallSearchParamChanged("status", status);
        }
    },

    onTmTextUnitCreatedBeforeChange(tmTextUnitCreatedBefore) {
                
        if (typeof tmTextUnitCreatedBefore === "string") {
            tmTextUnitCreatedBefore = moment(tmTextUnitCreatedBefore);
        }
        
        tmTextUnitCreatedBefore = tmTextUnitCreatedBefore.toISOString();
        
        this.setState({
            tmTextUnitCreatedBeforeTyping: null
        });
        
        if (tmTextUnitCreatedBefore !== this.state.tmTextUnitCreatedBefore ) {           
            this.setStateAndCallSearchParamChanged('tmTextUnitCreatedBefore', tmTextUnitCreatedBefore);
        };
        
    },

    onTmTextUnitCreatedAfterChange(tmTextUnitCreatedAfter) {

        if (typeof tmTextUnitCreatedAfter === "string") {
            tmTextUnitCreatedAfter = moment(tmTextUnitCreatedAfter);
        }

        tmTextUnitCreatedAfter = tmTextUnitCreatedAfter.toISOString();

        this.setState({
            tmTextUnitCreatedAfterTyping: null
        });

        if (tmTextUnitCreatedAfter !== this.state.tmTextUnitCreatedAfter ) {
            this.setStateAndCallSearchParamChanged('tmTextUnitCreatedAfter', tmTextUnitCreatedAfter);
        };

    },

    setStateAndCallSearchParamChanged(searchFilterParam, searchFilterParamValue) {
        let state = {};

        state[searchFilterParam] = searchFilterParamValue;
        
        this.setState(state, function () {
            this.callSearchParamChanged(searchFilterParam, searchFilterParamValue);
        });
    },

    callSearchParamChanged(searchFilterParam, searchFilterParamValue) {
        let actionData = {
            "changedParam": SearchConstants.SEARCHFILTER_CHANGED,
            "searchFilterParam": searchFilterParam,
            "searchFilterParamValue": searchFilterParamValue
        };

        WorkbenchActions.searchParamsChanged(actionData);
    },

    /**
     * When a filter is selected, update the search params
     *
     * @param filter selected filter
     */
    onFilterSelected(filter) {

        let newFilterValue = !this.state[filter];

        let actionData = {
            "changedParam": SearchConstants.SEARCHFILTER_CHANGED,
            "searchFilterParam": filter,
            "searchFilterParamValue": newFilterValue
        };

        WorkbenchActions.searchParamsChanged(actionData);
    },

    /**
     * Renders the filter menu item.
     *
     * @param filter
     * @param isYes
     * @returns {XML}
     */
    renderFilterMenuItem(filter, isYes) {

        let msg = isYes ? this.props.intl.formatMessage({ id: "search.statusDropdown.yes" }) : this.props.intl.formatMessage({ id: "search.statusDropdown.no" });

        return (
            <MenuItem eventKey={filter} active={this.state[filter]} onSelect={this.onFilterSelected} >{msg}</MenuItem>
        );
    },

    getMessageForStatus(status) {
        switch (status) {
            case SearchParamsStore.STATUS.ALL:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.all" });
            case SearchParamsStore.STATUS.TRANSLATED:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.translated" });
            case SearchParamsStore.STATUS.UNTRANSLATED:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.untranslated" });
            case SearchParamsStore.STATUS.FOR_TRANSLATION:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.forTranslation" });
            case SearchParamsStore.STATUS.MACHINE_TRANSLATED:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.machineTranslated" });
            case SearchParamsStore.STATUS.MT_REVIEW_NEEDED:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.mtReviewNeeded" });
            case SearchParamsStore.STATUS.REVIEW_NEEDED:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.needsReview" });
            case SearchParamsStore.STATUS.REJECTED:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.rejected" });
            case SearchParamsStore.STATUS.OVERRIDDEN:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.overridden" });
        }
    },

    renderStatusMenuItem(status) {
        return (
            <MenuItem eventKey={status} active={this.state.status === status} onSelect={this.onStatusSelected} >
                       {this.getMessageForStatus(status)}
            </MenuItem>
        );
    },

    getCreatedBeforeLocalDate() {      
       let m = moment(this.state.tmTextUnitCreatedBefore);
       return m;
    },

    getCreatedAfterLocalDate() {
        let m = moment(this.state.tmTextUnitCreatedAfter);
        return m;
    },

    renderCreatedBeforeInput(props) {
        function clear(){
            props.onChange({target: {value: ''}});
        }
               
        return (
            <InputGroup>
                <FormControl 
                    onClick={props.onClick} 
                    value={this.state.tmTextUnitCreatedBeforeTyping !== null ? this.state.tmTextUnitCreatedBeforeTyping : props.value}
                    placeholder={this.props.intl.formatMessage({ id: "search.statusDropdown.enterDate" })} 
                    onChange={ (e) => { 
                         this.setState({ 'tmTextUnitCreatedBeforeTyping': e.target.value});
                    }}
                    onKeyDown={ (e) => {
                        if (e.keyCode == keycode("enter")) {
                            props.onChange(e);
                        }                 
                    }}  />
                <InputGroup.Button>
                    <Button onClick={clear} disabled={!props.value && !this.state.tmTextUnitCreatedBeforeTyping}>
                        <Glyphicon glyph='glyphicon glyphicon-remove'/>
                    </Button>
                </InputGroup.Button>
            </InputGroup>
        );
    },

    renderCreatedAfterInput(props) {
        function clear(){
            props.onChange({target: {value: ''}});
        }

        return (
            <InputGroup>
                <FormControl
                    onClick={props.onClick}
                    value={this.state.tmTextUnitCreatedAfterTyping !== null ? this.state.tmTextUnitCreatedAfterTyping : props.value}
                    placeholder={this.props.intl.formatMessage({ id: "search.statusDropdown.enterDate" })}
                    onChange={ (e) => {
                        this.setState({ 'tmTextUnitCreatedAfterTyping': e.target.value});
                    }}
                    onKeyDown={ (e) => {
                        if (e.keyCode == keycode("enter")) {
                            props.onChange(e);
                        }
                    }}  />
                <InputGroup.Button>
                    <Button onClick={clear} disabled={!props.value && !this.state.tmTextUnitCreatedAfterTyping}>
                        <Glyphicon glyph='glyphicon glyphicon-remove'/>
                    </Button>
                </InputGroup.Button>
            </InputGroup>
        );
    },

    render() {  
        
        return (
                
            <DropdownButton  
                    id="WorkbenchStatusDropdown" 
                    title={this.props.intl.formatMessage({ id: "search.statusDropdown.title" })}
                    >

                <MenuItem header><FormattedMessage id="search.statusDropdown.status" /></MenuItem>
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.ALL)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.TRANSLATED)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.UNTRANSLATED)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.FOR_TRANSLATION)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.MACHINE_TRANSLATED)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.MT_REVIEW_NEEDED)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.REVIEW_NEEDED)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.REJECTED)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.OVERRIDDEN)}

                <MenuItem divider />

                <MenuItem header><FormattedMessage id="search.statusDropdown.used" /></MenuItem>
                    {this.renderFilterMenuItem("used", true)}
                    {this.renderFilterMenuItem("unUsed", false)}

                <MenuItem divider />

                <MenuItem header><FormattedMessage id="search.statusDropdown.translate" /></MenuItem>
                    {this.renderFilterMenuItem("translate", true)}
                    {this.renderFilterMenuItem("doNotTranslate", false)}
                            
                <MenuItem divider />
            
                <MenuItem header><FormattedMessage id="search.statusDropdown.tmTextUnitCreatedBefore" /></MenuItem>
                <MenuItem header className="prs pls"> 
                    <DateTime 
                        id="created-before-datepicker"
                        value={this.getCreatedBeforeLocalDate()}
                        onChange={this.onTmTextUnitCreatedBeforeChange}    
                        disableOnClickOutside={true}
                        closeOnSelect={true}
                        renderInput={ this.renderCreatedBeforeInput } 
                        />
                </MenuItem>

                <MenuItem header><FormattedMessage id="search.statusDropdown.tmTextUnitCreatedAfter" /></MenuItem>
                <MenuItem header className="prs pls">
                    <DateTime
                        id="created-after-datepicker"
                        value={this.getCreatedAfterLocalDate()}
                        onChange={this.onTmTextUnitCreatedAfterChange}
                        disableOnClickOutside={true}
                        closeOnSelect={true}
                        renderInput={ this.renderCreatedAfterInput }
                    />
                </MenuItem>
               
            </DropdownButton>
        );
    },
});


export default injectIntl(StatusDropdown);
