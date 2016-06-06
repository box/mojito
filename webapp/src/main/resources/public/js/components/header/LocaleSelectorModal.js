import $ from "jquery";
import React from "react/addons";
import ReactIntl from 'react-intl';
import {Button, Modal, ListGroup, ListGroupItem} from "react-bootstrap";

import { Router, Route, Link } from 'react-router'

import Locales from '../../utils/Locales';


let {IntlMixin} = ReactIntl;

let LocaleSelectorModal = React.createClass({

    mixins: [IntlMixin, React.addons.LinkedStateMixin],

    propTypes: {
        "onClose": React.PropTypes.func.isRequired
    },

    getInitialState() {
        return {
            "selectedLocale": Locales.getCurrentLocale()
        }
    },

    /**
     * Changes the locale of the app by setting the locale cookie and reloading the page.
     */
    onSaveClicked() {
        document.cookie = 'locale=' + this.state.selectedLocale;
        document.location.reload(true);
    },

    /**
     * Reset the selected locale and calls the onClose callback
     */
    close() {
        this.props.onClose();
    },

    /**
     * Indicates if the locale
     *
     * @returns {boolean}
     */
    isNewLocaleSelected() {
        return Locales.getCurrentLocale() !== this.state.selectedLocale;
    },

    /**
     * Selects the locale based on the list item that was clicked
     *
     * @param {SyntheticEvent} e The event object for the click event on text unit action options
     */
    onLocaleClicked(e) {
        let selectedLocale = $(e.target).data('value');

        this.setState({
            "selectedLocale": selectedLocale
        });
    },


    getLocaleListGroupItem(locale) {

        let localeDisplayName = Locales.getNativeDispalyName(locale);
        let active = locale === this.state.selectedLocale;

        return (
            <ListGroupItem active={active} onClick={this.onLocaleClicked} data-value={locale}>{localeDisplayName}</ListGroupItem>
        );
    },

    getLocaleListGroup() {

        let localeListGroupItems = Locales.getSupportedLocales().map(this.getLocaleListGroupItem);

        return (
            <ListGroup>{localeListGroupItems}</ListGroup>
        );
    },

    render() {
        return (
            <Modal show={this.props.show} onHide={this.close}>
                <Modal.Header closeButton>
                    <Modal.Title>{this.getIntlMessage("localeselector.title")}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {this.getLocaleListGroup()}
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.close}>
                        {this.getIntlMessage("label.cancel")}
                    </Button>
                    <Button bsStyle="primary" onClick={this.onSaveClicked}
                        disabled={!this.isNewLocaleSelected()}>
                        {this.getIntlMessage("label.save")}
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
});

export default LocaleSelectorModal;
