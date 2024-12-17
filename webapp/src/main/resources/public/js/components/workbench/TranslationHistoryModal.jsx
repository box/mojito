import PropTypes from "prop-types";
import React from "react";
import { FormattedMessage, injectIntl } from "react-intl";
import { Button, Glyphicon, Modal } from "react-bootstrap";
import { Table } from "react-bootstrap";
import StatusGlyph from "../widgets/StatusGlyph";
import TextUnitSDK from "../../sdk/TextUnit";

class translationHistoryModal extends React.Component {
    static propTypes() {
        return {
            show: PropTypes.bool.isRequired,
            textUnit: PropTypes.object.isRequired,
            translationHistory: PropTypes.object.isRequired,
            onChangeOpenTmTextUnitVariant: PropTypes.func.isRequired,
        };
    }

    renderHistoryItem = (item) => {
        const { textUnit, openTmTextUnitVariantId } = this.props;
        const rowClass = textUnit.getTmTextUnitVariantId() === item.id ? "history-current-variant" : "";
        const mtStatus = item.status === TextUnitSDK.STATUS.MACHINE_TRANSLATED || item.status === TextUnitSDK.STATUS.MT_REVIEW_NEEDED;
        const status = item.id && !item.includedInLocalizedFile && !mtStatus ? TextUnitSDK.STATUS.REJECTED : item.status;
        const isOpenTmTextUnitVariant = openTmTextUnitVariantId === item.id;

        return item ? (
            <React.Fragment key={item.id}>
                <tr className={rowClass} key={`${item.id}-1`}>
                    <td>
                        {item.tmTextUnitVariantComments.length == 0 ? (
                            ""
                        ) : (
                            <Button
                                bsSize="xsmall"
                                onClick={() => this.props.onChangeOpenTmTextUnitVariant(isOpenTmTextUnitVariant ? null : item.id)}
                            >
                                <Glyphicon
                                    glyph={isOpenTmTextUnitVariant ? "chevron-down" : "chevron-right"}
                                    className="color-gray-light"
                                />
                            </Button>
                        )}
                    </td>
                    <td className="history-none">
                        {item.createdByUser === null ? (
                            <FormattedMessage id="textUnit.translationHistoryModal.NoUser" />
                        ) : (
                            item.createdByUser.username
                        )}
                    </td>
                    <td>{item.content}</td>
                    <td>{this.convertDateTime(item.createdDate)}</td>
                    <td>
                        <StatusGlyph status={status} noButton={true} onClick={() => ""} />
                    </td>
                </tr>
                {isOpenTmTextUnitVariant ? (
                    <tr key={`${item.id}-2`}>
                        <td colSpan={5}>
                            <Table>
                                <thead>
                                <tr>
                                    <th className="col-md-1">type</th>
                                    <th className="col-md-1">severity</th>
                                    <th className="col-md-14">content</th>
                                </tr>
                                </thead>
                                <tbody>
                                {item.tmTextUnitVariantComments.map((tmTextUnitVariantComment) => (
                                    <tr key={tmTextUnitVariantComment.id}>
                                        <td>{tmTextUnitVariantComment.type}</td>
                                        <td>{tmTextUnitVariantComment.severity}</td>
                                        <td>{tmTextUnitVariantComment.content}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </Table>
                        </td>
                    </tr>
                ) : (
                    ""
                )}
            </React.Fragment>
        ) : (
            ""
        );
    };

    /**
     * @returns {*} Generated content for the git blame information section
     */
    rendertranslationHistory = () => {
        const { translationHistory, textUnit, intl, openTmTextUnitVariantId } = this.props;

        return (
            <Table className="repo-table table-padded-sides">
                <thead>
                <tr>
                    <th className="col-md-1"></th>
                    <th className="col-md-4">
                        <FormattedMessage id="textUnit.translationHistoryModal.User" />
                    </th>
                    <th className="col-md-4">
                        <FormattedMessage id="textUnit.translationHistoryModal.Translation" />
                    </th>
                    <th className="col-md-4">
                        <FormattedMessage id="textUnit.translationHistoryModal.Date" />
                    </th>
                    <th className="col-md-3">
                        <FormattedMessage id="textUnit.translationHistoryModal.Status" />
                    </th>
                </tr>
                </thead>
                <tbody>
                {translationHistory && translationHistory.length ? translationHistory.map(this.renderHistoryItem.bind(this)) : ""}
                <tr key={`${textUnit.id}-0`}>
                    <td></td>
                    <td>-</td>
                    <td>
                            <span className="history-none">
                                <FormattedMessage id="textUnit.translationHistoryModal.InitialPush" />
                            </span>
                    </td>
                    <td>{this.convertDateTime(textUnit.getCreatedDate())}</td>
                    <td></td>
                </tr>
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
            year: "numeric",
            month: "numeric",
            day: "numeric",
            hour: "numeric",
            minute: "numeric",
        };
        return this.props.intl.formatDate(date, options);
    };

    /**
     * Closes the modal and calls the parent action handler to mark the modal as closed
     */
    closeModal = () => {
        this.props.onCloseModal();
    };

    render() {
        const { textUnit, show, translationHistory } = this.props;
        const translationExists = translationHistory && translationHistory.length && textUnit.getTmTextUnitId();
        const translationIsNotLatest = translationExists && textUnit.getTmTextUnitVariantId() !== translationHistory[0].id;

        return textUnit ? (
            <Modal className={"git-blame-modal"} show={show} onHide={this.closeModal}>
                <Modal.Header closeButton>
                    <Modal.Title>
                        <FormattedMessage id={"workbench.translationHistoryModal.title"} />
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div className={"row"}>
                        <div className={"history-source"}>{textUnit.getSource()}</div>
                    </div>
                    <div className={"row plx"}>
                        {translationIsNotLatest ? <FormattedMessage id={"textUnit.translationHistoryModal.translationNotLatest"} /> : ""}
                    </div>
                    <p />
                    <div>{this.rendertranslationHistory()}</div>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="primary" onClick={this.closeModal}>
                        <FormattedMessage id={"textUnit.translationHistoryModal.close"} />
                    </Button>
                </Modal.Footer>
            </Modal>
        ) : (
            ""
        );
    }
}

export default injectIntl(translationHistoryModal);
