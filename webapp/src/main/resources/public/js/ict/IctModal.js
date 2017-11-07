import React from 'react';
import {Button, Modal, Panel, Glyphicon, Label} from "react-bootstrap";
import {FormattedMessage, injectIntl} from "react-intl";
import ClassNames from "classnames";


class ModalTextUnit extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            showStack: false
        };
    }
    
    render() {
        var textUnit = this.props.textUnit;

        var className = ClassNames("mbs", "pbs", "pts", "pls", "prs",
           {"selected": this.props.selected });

        return (
            <div className={className} 
                 onClick={() => this.props.onSelect(textUnit)}>
                <div className="mbs">
                    <Label className="mbs clickable label label-primary mrs">
                        {textUnit.locale}            
                    </Label>  
                    <span className="color-gray-light3">{textUnit.textUnitName}</span>
                </div>
          
                <div><Glyphicon glyph="folder-open" className="color-gray-light mrs"/>{textUnit.repositoryName}</div>
                <div><Glyphicon glyph="file" className="color-gray-light mrs"/>{textUnit.assetName}</div>
                
                <div className="mts em color-gray-light">{textUnit.textUnitVariant}</div>
                <Button className="mtm mbs" onClick={() => { this.setState({showStack: !this.state.showStack})}}>Stack</Button>
                {this.state.showStack && <Panel collapsible expanded={this.state.showStack}>{textUnit.stack}</Panel> }
            </div>
        );   
    } 
};

class IctModal extends React.Component {
      
    render() {
        return (
            <Modal show={this.props.show} onHide={this.props.onClose} container={this.props.container}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id="ict.modal.title" /></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {this.props.textUnits && this.props.textUnits.map((textUnit) => 
                        <ModalTextUnit 
                            key={textUnit.textUnitName} 
                            textUnit={textUnit} 
                            selected={this.props.selectedTextUnit === textUnit}
                            onSelect={this.props.onSelectTextUnit}/>
                    )}           
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="primary" onClick={this.props.onOkay}>
                        <FormattedMessage id="label.okay"/>
                    </Button>
                    <Button onClick={this.props.onClose}>
                        <FormattedMessage id="label.cancel"/>
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }; 
};

export default injectIntl(IctModal);