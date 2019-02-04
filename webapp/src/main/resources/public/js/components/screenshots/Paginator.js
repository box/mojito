import PropTypes from 'prop-types';
import React from "react";
import {injectIntl} from 'react-intl';
import {Button} from "react-bootstrap";


class Paginator extends React.Component {
  
    static propTypes = {
        "currentPageNumber": PropTypes.number.isRequired,
        "hasNextPage": PropTypes.bool.isRequired,
        "onPreviousPageClicked": PropTypes.func.isRequired,
        "onNextPageClicked": PropTypes.func.isRequired,
        "disabled": PropTypes.bool.isRequired,
        "shown": PropTypes.bool.isRequired,
    }

    /**
     * @return {JSX}
     */
    render() {
        return (this.props.shown &&
                <div className="screenshot-paginator">
                    <Button bsSize="small" disabled={this.props.disabled || this.props.currentPageNumber === 1}
                            onClick={this.props.onPreviousPageClicked}><span
                            className="glyphicon glyphicon-chevron-left"></span></Button>
                    <label className="mls mrs default-label current-pageNumber">
                        {this.props.currentPageNumber}
                    </label>
                    <Button bsSize="small" disabled={this.props.disabled || !this.props.hasNextPage}
                            onClick={this.props.onNextPageClicked}><span
                            className="glyphicon glyphicon-chevron-right"></span></Button>
                </div>
                );
    }

};

export default injectIntl(Paginator);
