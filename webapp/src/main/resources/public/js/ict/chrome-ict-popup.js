import React from "react";
import ReactDOM from "react-dom";

import {Button, ToggleButton, ToggleButtonGroup, Form, FormGroup, FormControl, 
    ControlLabel, Col} from "react-bootstrap";

import "../../../sass/mojito.scss";
import "../../../sass/ict.scss";

const IctPopup = function (props) {
    
    return <div className="mojito-ict-popup plm prm mbm">
        <Form horizontal>
            <ToggleButtonGroup className="mtm mbm" name="state" type="radio" value={props.enabled} 
                               onChange={props.onEnabledChanged} >
                <ToggleButton name="state" value={true}>On</ToggleButton>
                <ToggleButton name="state" value={false}>Off</ToggleButton>
            </ToggleButtonGroup> 
            
            <FormGroup controlId="formHorizontalMojitoUrl">
                <Col componentClass={ControlLabel} sm={2}>
                Mojito Base URL
                </Col>
              <Col sm={10}>
                <FormControl type="text" value={props.mojitoBaseUrl} onChange={(e) => props.onMojitoBaseUrlChanged(e.target.value)} />
              </Col>
            </FormGroup>

            <FormGroup controlId="formHorizontalHeaderName">
                <Col componentClass={ControlLabel} sm={2}>
                Header Name
                </Col>
              <Col sm={10}>
                <FormControl type="text" value={props.headerName} onChange={(e) => props.onHeaderNameChanged(e.target.value)} />
              </Col>
            </FormGroup>
            
            <FormGroup controlId="formHorizontalHeaderValue">
                <Col componentClass={ControlLabel} sm={2}>
                Header Value
                </Col>
              <Col sm={10}>
                <FormControl type="text" value={props.headerValue} onChange={(e) => props.onHeaderValueChanged(e.target.value)} />
              </Col>
            </FormGroup>
            
            <FormGroup controlId="formHorizontalRemoveTagsBlock">
                <Col componentClass={ControlLabel} sm={2}>
                    Remove meta characters
                </Col>
                <Col sm={10}>
                    <ToggleButtonGroup name="state" type="radio" 
                               value={props.removeTagsBlock} 
                               onChange={props.onRemoveTagsBlockChanged} >
                        <ToggleButton name="state" value={true}>On</ToggleButton>
                        <ToggleButton name="state" value={false}>Off</ToggleButton>
                    </ToggleButtonGroup> 
                 </Col>
            </FormGroup>
        </Form>
    </div>;
}

class IctPopupContainer extends React.Component {
    
    constructor() {
        super();
        
        this.state = {
            mojitoBaseUrl: '',
            enabled: false,
            headerName: '',
            headerValue: '',
            removeTagsBlock: true
        };
        
        this.loadStateFromStorage();
    }
    
    loadStateFromStorage() {
        chrome.storage.sync.get(this.state, (items) => {
            this.setState(items);
        });
    }
 
    onMojitoBaseUrlChanged(mojitoBaseUrl) {
        this.setState({
            mojitoBaseUrl:  mojitoBaseUrl
        });

        chrome.storage.sync.set({
            mojitoBaseUrl: mojitoBaseUrl
        });
    }
    
    onHeaderNameChanged(headerName) {
        this.setState({
            headerName: headerName
        });

        chrome.storage.sync.set({
            headerName: headerName
        });
    }
    
    onHeaderValueChanged(headerValue) {
        this.setState({
            headerValue: headerValue
        });

        chrome.storage.sync.set({
            headerValue: headerValue
        });
    }
    
    onEnabledChanged(enabled) {
        this.setState({
            enabled: enabled
        });

        chrome.storage.sync.set({
            enabled: enabled
        });
    }
    
    onRemoveTagsBlockChanged(removeTagsBlock) {
        this.setState({
            removeTagsBlock: removeTagsBlock
        });

        chrome.storage.sync.set({
            removeTagsBlock: removeTagsBlock
        });
    }
    
    render() {
        return <IctPopup
                    mojitoBaseUrl={this.state.mojitoBaseUrl}
                    enabled={this.state.enabled}
                    headerName={this.state.headerName}
                    headerValue={this.state.headerValue}
                    removeTagsBlock={this.state.removeTagsBlock}
                    onMojitoBaseUrlChanged={url => this.onMojitoBaseUrlChanged(url)}
                    onHeaderNameChanged={name => this.onHeaderNameChanged(name)}
                    onHeaderValueChanged={value => this.onHeaderValueChanged(value)}
                    onEnabledChanged={enabled => this.onEnabledChanged(enabled)}
                    onRemoveTagsBlockChanged={removeTagsBlock => this.onRemoveTagsBlockChanged(removeTagsBlock)}
                />;
                    
    }
}

ReactDOM.render(
        <IctPopupContainer />
        , document.getElementById("app")
        );
