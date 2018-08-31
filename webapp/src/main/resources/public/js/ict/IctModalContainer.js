import React from 'react';
import IctModal from "./IctModal";
import queryString from "query-string";

class IctModalContainer extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            showModal: false,
            mojitoBaseUrl: this.props.defaultMojitoBaseUrl,
            actionButtons: this.props.actionButtons
        };
    }

    showModal(textUnits) {
        this.setState({
            showModal: true,
            textUnits: textUnits,
            selectedTextUnit: textUnits[0]
        });
    }

    closeModal() {
        this.setState({
            showModal: false
        });
    }
    
    setMojitoBaseUrl(mojitoBaseUrl) {
        this.setState({
            mojitoBaseUrl: mojitoBaseUrl
        });
    }

    setActionButtons(actionButtons) {
        this.setState({
            actionButtons: actionButtons
        });
    }

    openWorkbench() {
        var query = queryString.stringify({
            'repoNames[]': this.state.selectedTextUnit.repositoryName,
            'bcp47Tags[]': this.state.selectedTextUnit.locale,
            'searchAttribute': 'stringId',
            'searchType': 'exact',
            'searchText': this.state.selectedTextUnit.textUnitName
        });
        window.open(this.state.mojitoBaseUrl + "workbench?" + query);
    }

    openActionInNewWindow(url) {
        var query = queryString.stringify({
            'repoName': this.state.selectedTextUnit.repositoryName,
            'locales': this.state.selectedTextUnit.locale,
            'searchText': this.state.selectedTextUnit.textUnitName,
            'assetName': this.state.selectedTextUnit.assetName
        });
        window.open(url + query);
    }

    render() {
        return (
                <div className="mojito-ict-modal">
                    <IctModal
                        show={this.state.showModal}
                        actionButtons={this.state.actionButtons}
                        onClose={() => this.closeModal()}
                        onOkay={() => this.openWorkbench()}
                        onOpenAction={(url) => this.openActionInNewWindow(url)}
                        onSelectTextUnit={(textUnit) => this.setState({selectedTextUnit : textUnit})} 
                        textUnits={this.state.textUnits}
                        selectedTextUnit={this.state.selectedTextUnit}
                        container={this}
                        />
                </div>
        );
    }
}

export default IctModalContainer;