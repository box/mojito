import PropTypes from 'prop-types';
import React from "react";
import {injectIntl} from 'react-intl';
import {Label} from "react-bootstrap";


class ScreenshotsTextUnit extends React.Component {
    
    static propTypes = {
        "textUnit": PropTypes.object.isRequired,
        "onNameClick": PropTypes.func.isRequired,
        "onTargetClick": PropTypes.func.isRequired,
    }

    renderNumberOfMatches() {
       var textNumberOfMatch = "";
       
       if (this.props.textUnit.numberOfMatch !== 1) {
           textNumberOfMatch = <span className='em color-gray-light2'> ({this.props.textUnit.numberOfMatch})</span>;
       }
       
       return textNumberOfMatch;
    }

    /**
     * @return {JSX}
     */
    render() {
        return (
                <div className='screenshot-textunit'>
                    <div className='mbxs'>
                        <Label bsStyle='primary' bsSize='large' className="mrxs mtl clickable" onClick={this.props.onNameClick}>
                            {this.props.textUnit.name}
                        </Label> 
                    </div>
                    <div className='em color-gray-light2'>{this.props.textUnit.source}</div>
                    <div className="clickable" onClick={this.props.onTargetClick}>{this.props.textUnit.renderedTarget}
                    {this.props.textUnit.numberOfMatch !== 1 && 
                         <span className='em color-gray-light2'> ({this.props.textUnit.numberOfMatch})</span>}
                    </div> 
                </div>
                );
    }
};

export default injectIntl(ScreenshotsTextUnit);
