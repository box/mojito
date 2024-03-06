import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage} from "react-intl";
import {Button, Modal, ListGroup, ListGroupItem} from "react-bootstrap";
import Locales from "../../utils/Locales";

class LocaleSelectorModal extends React.Component {
    static propTypes = {
        "onClose": PropTypes.func.isRequired
    };

    state = {
        "localeInput": Locales.getCurrentLocale()
    };

    /**
     * Changes the locale of the app by setting the locale cookie and reloading the page.
     */
    onSaveClicked = () => {
        document.cookie = 'locale=' + this.state.localeInput;
        document.location.reload(true);
    };

    /**
     * Reset the selected locale and calls the onClose callback
     */
    close = () => {
        this.props.onClose();
    };

    /**
     * Indicates if the locale
     *
     * @returns {boolean}
     */
    isNewLocaleSelected = () => {
        return Locales.getCurrentLocale() !== this.state.localeInput;
    };

    /**
     * Selects the locale based on the list item that was clicked
     *
     * @param {string} locale
     */
    onLocaleClicked = (locale) => {
        this.setState({
            "localeInput": locale
        });
    };

    /**
     *
     * @param {string} locale BCP47 Tag
     * @return {XML}
     */
    getLocaleListGroupItem = (locale) => {

        let localeDisplayName = Locales.getNativeDispalyName(locale);
        let active = locale === this.state.localeInput;

        return (
            <ListGroupItem active={active}
                           onClick={(e) => {
                               e.stopPropagation();
                               this.onLocaleClicked(locale);
                           }}
                           key={locale}>{localeDisplayName}
            </ListGroupItem>
        );
    };

    getLocaleListGroup = () => {

        let localeListGroupItems = Locales.getSupportedLocales().map(this.getLocaleListGroupItem);

        return (
            <ListGroup>{localeListGroupItems}</ListGroup>
        );
    };

    render() {
        return (
            <Modal show={this.props.show} onHide={this.close}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id="localeselector.title"/></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {this.getLocaleListGroup()}
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.close}>
                        <FormattedMessage id="label.cancel"/>
                    </Button>
                    <Button bsStyle="primary" onClick={this.onSaveClicked}
                            disabled={!this.isNewLocaleSelected()}>
                        <FormattedMessage id="label.save"/>
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

export default LocaleSelectorModal;
