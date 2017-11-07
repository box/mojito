import React from 'react';
import {Button, Modal} from "react-bootstrap";
import {FormattedMessage, injectIntl} from "react-intl";
import ClassNames from "classnames";


const ModalTextUnit = (props) => {
    var textUnit = props.textUnit;

    var className = ClassNames("mbs", "pbs", "pts", "pls", "prs",
       {"selected": props.selected });

    return (
        <div className={className} 
             onClick={() => props.onSelect(textUnit)}>

            <div>Repository: {textUnit.repositoryName}</div>
            <div>Asset: {textUnit.assetName ? textUnit.assetName : " - " }</div>
            <div>String Id: {textUnit.textUnitName}</div>
            <div>Locale: {textUnit.locale}</div>
            <div>Translation: {textUnit.textUnitVariant}</div>
        </div>
    );    
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