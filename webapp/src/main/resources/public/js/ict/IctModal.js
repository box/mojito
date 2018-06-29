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
    showButtons(actionButtons) {
        var buttons = "";
        if (Array.isArray(actionButtons)) {
            buttons = actionButtons.map((button) =>
                <Button key={button.text} onClick={() => this.props.onOpenAction(button.url)}>
                    {button['text']}
                </Button>
            );
        }
        return buttons;
    }
    render() {
        // include <style> here because css can't have precedence over <style>
        // from apps that override fonts, ect
        return (
            <div>   
                <style>
                    {"#mojito-ict * {font-family: \"Helvetica Neue\", Helvetica, Arial, sans-serif, \"GLYPHICONS Halflings\" !important; margin: 0; outline: none; letter-spacing: normal !important;}"}
                </style>
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
                        {this.showButtons(this.props.actionButtons)}
                        <Button bsStyle="primary" onClick={this.props.onOkay}>
                            <FormattedMessage id="label.okay"/>
                        </Button>
                        <Button onClick={this.props.onClose}>
                            <FormattedMessage id="label.cancel"/>
                        </Button>
                    </Modal.Footer>
                </Modal>
            </div>
        );
    }; 
};

export default injectIntl(IctModal);