import React from "react";
import {injectIntl} from "react-intl";
import PropTypes from "prop-types";
import keycode from "keycode";
import {Button, FormControl, FormGroup, Glyphicon, InputGroup} from "react-bootstrap";

class SearchText extends React.Component {
    static propTypes = {
        "searchText": PropTypes.string.isRequired,
        "isSpinnerShown": PropTypes.bool.isRequired,
        "onSearchTextChanged": PropTypes.func.isRequired,
        "onPerformSearch": PropTypes.func.isRequired,
        "placeholderTextId": PropTypes.string.isRequired
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

    renderSearchButton() {
        return (
            <Button onClick={() => this.onSearchButtonClicked()}>
                <Glyphicon glyph='glyphicon glyphicon-search'/>
            </Button>
        );
    }

    render() {
        return (
            <div className="search-text">
                <FormGroup>
                    <InputGroup>
                        <FormControl type='text' value={this.props.searchText}
                                     onChange={(e) => this.props.onSearchTextChanged(e.target.value)}
                                     placeholder={this.props.intl.formatMessage({id: this.props.placeholderTextId})}
                                     onKeyDown={(e) => this.onKeyDownOnSearchText(e)}/>
                        <InputGroup>
                            {this.props.isSpinnerShown && (<span className="glyphicon glyphicon-refresh spinning"/>)}
                        </InputGroup>
                        <InputGroup.Button>{this.renderSearchButton()}</InputGroup.Button>
                    </InputGroup>
                </FormGroup>
            </div>
        );
    }
}
export default injectIntl(SearchText);