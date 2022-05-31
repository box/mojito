import React from "react";
import PropTypes from 'prop-types';
import {FormattedMessage, injectIntl} from 'react-intl';
import {Button, DropdownButton, FormControl, Glyphicon, InputGroup, MenuItem} from "react-bootstrap";
import DateTime from "react-datetime";
import moment from 'moment';
import keycode from "keycode";


class BranchesStatusDropdown extends React.Component {

    static propTypes = {
        "deleted": PropTypes.bool.isRequired,
        "undeleted": PropTypes.bool.isRequired,
        "empty": PropTypes.bool.isRequired,
        "notEmpty": PropTypes.bool.isRequired,
        "onlyMyBranches": PropTypes.bool.isRequired,
        "onDeletedChanged": PropTypes.func.isRequired,
        "onUndeletedChanged": PropTypes.func.isRequired,
        "onEmptyChanged": PropTypes.func.isRequired,
        "onNotEmptyChanged": PropTypes.func.isRequired,
        "onOnlyMyBranchesChanged": PropTypes.func.isRequired,
        "createdBefore": PropTypes.object,
        "createdAfter": PropTypes.object,
        "onCreatedBeforeChanged": PropTypes.func.isRequired,
        "onCreatedAfterChanged": PropTypes.func.isRequired,
    }

    renderFilterMenuItem(filter, isYes, prop, callback) {

        let msg = isYes ? this.props.intl.formatMessage({id: "search.statusDropdown.yes"}) : this.props.intl.formatMessage({id: "search.statusDropdown.no"});

        return (
            <MenuItem eventKey={filter} active={prop}
                      onSelect={() => {
                          callback(!prop)
                      }}>
                {msg}
            </MenuItem>
        );
    }

    renderFilterMenuDateItem(filter, callback) {
        return (
            <MenuItem header className="prs pls">
                <InputGroup>
                    <DateTime
                        id={`branches-${filter}-filter`}
                        value={this.props[filter]}
                        onChange={callback}
                        disableOnClickOutside={true}
                        closeOnSelect={true}
                        inputProps={{
                            placeholder: this.props.intl.formatMessage({
                                id: "search.statusDropdown.enterDate"
                            })
                        }}
                    />
                    <InputGroup.Button>
                        <Button onClick={() => callback(null)} disabled={!this.props[filter]}>
                            <Glyphicon glyph='glyphicon glyphicon-remove'/>
                        </Button>
                    </InputGroup.Button>
                </InputGroup>
            </MenuItem>
        );
    }

    render() {
        return (
            <DropdownButton id="BranchesStatusDropdown"
                            title={this.props.intl.formatMessage({id: "search.statusDropdown.title"})}>

                <MenuItem header><FormattedMessage id="branches.searchstatusDropdown.owner"/></MenuItem>
                <MenuItem eventKey={"onlyMyBranches"} active={this.props.onlyMyBranches}
                          onSelect={() => this.props.onOnlyMyBranchesChanged(!this.props.onlyMyBranches)}>
                    <FormattedMessage id="branches.searchstatusDropdown.owner.onlyMyBranches"/>
                </MenuItem>

                <MenuItem divider/>

                <MenuItem header><FormattedMessage id="branches.searchstatusDropdown.deleted"/></MenuItem>
                {this.renderFilterMenuItem("deleted", true, this.props.deleted, this.props.onDeletedChanged)}
                {this.renderFilterMenuItem("undeleted", false, this.props.undeleted, this.props.onUndeletedChanged)}

                <MenuItem divider/>

                <MenuItem header><FormattedMessage id="branches.searchstatusDropdown.empty"/></MenuItem>
                {this.renderFilterMenuItem("empty", true, this.props.empty, this.props.onEmptyChanged)}
                {this.renderFilterMenuItem("notEmpty", false, this.props.notEmpty, this.props.onNotEmptyChanged)}

                <MenuItem divider/>

                <MenuItem header><FormattedMessage id="search.statusDropdown.tmTextUnitCreatedBefore"/></MenuItem>
                {this.renderFilterMenuDateItem("createdBefore", this.props.onCreatedBeforeChanged)}

                <MenuItem header><FormattedMessage id="search.statusDropdown.tmTextUnitCreatedAfter"/></MenuItem>
                {this.renderFilterMenuDateItem("createdAfter", this.props.onCreatedAfterChanged)}

            </DropdownButton>
        );
    }
};


export default injectIntl(BranchesStatusDropdown);
