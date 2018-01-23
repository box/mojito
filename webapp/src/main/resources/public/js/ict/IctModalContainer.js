import React from 'react';
import IctModal from "./IctModal";
import queryString from "query-string";

class IctModalContainer extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            showModal: false,
            mojitoBaseUrl: this.props.defaultMojitoBaseUrl
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

    render() {
        return (
                <div className="mojito-ict-modal">
                    <IctModal
                        show={this.state.showModal}
                        onClose={() => this.closeModal()}
                        onOkay={() => this.openWorkbench()}
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