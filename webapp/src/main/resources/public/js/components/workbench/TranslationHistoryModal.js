import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, Modal} from "react-bootstrap";
import {Table} from "react-bootstrap";
import StatusGlyph from '../widgets/StatusGlyph';

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
        const { translationHistory } = this.props;

        if (translationHistory && translationHistory.length) {
            return (
                <Table striped bordered hover responsive="sm">
                    <thead>
                        <th><FormattedMessage id="textUnit.translationHistoryModal.User"/></th>
                        <th><FormattedMessage id="textUnit.translationHistoryModal.Translation"/></th>
                        <th><FormattedMessage id="textUnit.translationHistoryModal.Date"/></th>
                        <th><FormattedMessage id="textUnit.translationHistoryModal.Status"/></th>
                    </thead>
                    <tbody>
                    {translationHistory.map(this.renderHistoryItem)}
                    </tbody>
                </Table>
            );
        } else {
            // for safety
            return <div><FormattedMessage id="textUnit.translationHistoryModal.NoTranslations"/></div>;
        }
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
                            <h4>
                                <FormattedMessage id={"textUnit.translationHistoryModal.header"} values={{
                                    locale: textUnit.getTargetLocale(),
                                    source: textUnit.getSource()
                                }}/>
                            </h4>
                        </div>
                    </div>
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
