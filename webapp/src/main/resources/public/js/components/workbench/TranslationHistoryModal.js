import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, Modal} from "react-bootstrap";
import {Table} from "react-bootstrap";
import StatusGlyph from '../widgets/StatusGlyph';
import TextUnitSDK from "../../sdk/TextUnit";

class translationHistoryModal extends React.Component {
    static propTypes() {
        return {
            "show": PropTypes.bool.isRequired,
            "textUnit": PropTypes.object.isRequired,
            "translationHistory": PropTypes.object.isRequired
        };
    }

    renderHistoryItem = (item) => {
        return item ?
            (
                <tr>
                    <td>{item.createdByUser.username}</td>
                    <td>{item.content}</td>
                    <td>{this.convertDateTime(item.createdDate)}</td>
                    <td><StatusGlyph status={item.status} onClick={() => ""}/></td>
                </tr>
            ) :
            "";
    };

    /**
     * @returns {*} Generated content for the git blame information section
     */
    rendertranslationHistory = () => {
        const { translationHistory, textUnit, intl } = this.props;

        return (
            <Table striped bordered hover responsive="sm">
                <thead>
                    <th><FormattedMessage id="textUnit.translationHistoryModal.User"/></th>
                    <th><FormattedMessage id="textUnit.translationHistoryModal.Translation"/></th>
                    <th><FormattedMessage id="textUnit.translationHistoryModal.Date"/></th>
                    <th><FormattedMessage id="textUnit.translationHistoryModal.Status"/></th>
                </thead>
                <tbody>
                {(translationHistory && translationHistory.length) ? translationHistory.map(this.renderHistoryItem) : ""}
                {this.renderHistoryItem({
                    createdByUser: {
                        username: "mojito"
                    },
                    content: (<span class="history-initial"><FormattedMessage id="textUnit.translationHistoryModal.InitialPush"/></span>),
                    createdDate: textUnit.getTmTextUnitCreatedDate(),
                    status: TextUnitSDK.STATUS.TRANSLATION_NEEDED
                })}
                </tbody>
            </Table>
        );
    };

    /**
     * @param date   An integer representing a datetime
     * @returns {*}  Human readable version of the given datetime
     *
     */
    convertDateTime = (date) => {
        if (date === null || isNaN(date)) {
            return null;
        }

        let options = {
            year: 'numeric', month: 'numeric', day: 'numeric',
            hour: 'numeric', minute: 'numeric'
        };
        return (this.props.intl.formatDate(date, options));
    };

    /**
     * Closes the modal and calls the parent action handler to mark the modal as closed
     */
    closeModal = () => {
        this.props.onCloseModal();
    };

    render() {
        const { textUnit, show } = this.props;

        return textUnit ? (
            <Modal className={"git-blame-modal"} show={show} onHide={this.closeModal}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id="workbench.translationHistoryModal.title"/></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div className={"row"}>
                        <div className={"col-sm-4"}>
                            <div>
                                <FormattedMessage id={"textUnit.translationHistoryModal.locale"} values={{
                                    locale: textUnit.getTargetLocale()
                                }}/>
                            </div>
                            <div>
                                <FormattedMessage id={"textUnit.translationHistoryModal.source"} values={{
                                    source: textUnit.getSource()
                                }}/>
                            </div>
                        </div>
                    </div>
                    <p/>
                    <div>
                        {this.rendertranslationHistory()}
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="primary" onClick={this.closeModal}>
                        <FormattedMessage id={"textUnit.translationHistoryModal.close"}/>
                    </Button>
                </Modal.Footer>
            </Modal>
        ) : "";
    }
}

export default injectIntl(translationHistoryModal);
