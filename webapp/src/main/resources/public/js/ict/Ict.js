import React from "react";
import ReactDOM from "react-dom";

import IctMetadataExtractor from "./IctMetadataExtractor";
import IctModalContainer from "./IctModalContainer";
import TagsBlockDecoder from './TagsBlockDecoder';
import {IntlProvider} from "react-intl";

class Ict {

    constructor(messages, locale) {
        this.mojitoBaseUrl = 'http://localhost:8080/';
        this.messages = messages;
        this.locale = locale;
        this.actionButtons = [];
        this.mtEnabled = false;
    }

    activate() {
        this.installInDOM();
        this.addMutationObserver();
        this.addScheduledUpdates();
    }

    setMojitoBaseUrl(mojitoBaseUrl) {
        this.mojitoBaseUrl = mojitoBaseUrl;
        if (this.ictModalContainer) {
            this.ictModalContainer.setMojitoBaseUrl(mojitoBaseUrl);
        }
    }

    setActionButtons(actionButtons) {
        this.actionButtons = actionButtons;
        if (this.ictModalContainer) {
            this.ictModalContainer.setActionButtons(actionButtons);
        }
    }

    setMTEnabled(mtEnabled) {
        this.mtEnabled = mtEnabled;
    }

    getNodesChildOf(node) {
        var nodes = [];

        for (node = node.firstChild; node; node = node.nextSibling) {
            if (node.nodeType === Node.TEXT_NODE) {
                nodes.push(node);
            } else if (this.isElementWithPlaceholder(node)) {
                nodes.push(node);
            } else {
                nodes = nodes.concat(this.getNodesChildOf(node));
            }
        }

        return nodes;
    }

    isElementWithPlaceholder(node) {
        return node.nodeType === Node.ELEMENT_NODE && this.getPlaceholderAttribute(node);
    }

    getPlaceholderAttribute(node) {
        return node.getAttribute('placeholder');
    }

    getStringFromNode(node) {

        var string = node.nodeValue;

        if (string && IctMetadataExtractor.hasPartialMetadata(string)) {
            // the input node doesn't contain the full ict metadata, it should
            // have sibblings that contain the end of the metadata. Usually 
            // this happens when HTML is injected into a message and then the 
            // string is spread accross multiple DOM nodes. In that case, the 
            // full string can be retreive by getting the parent node text 
            // content
            string = node.parentNode.textContent;
        }

        if (this.isElementWithPlaceholder(node)) {
            string = this.getPlaceholderAttribute(node);
        }

        return string;
    }

    wrapNode(node, onClick, addMTCSS) {
        var textUnits = IctMetadataExtractor.getTextUnits(this.getStringFromNode(node));

        if (node.nodeType === Node.TEXT_NODE) {
            node = node.parentNode;
        }

        var mtRequired = this.mtEnabled && textUnits[0]['translationType'] === 'MT_REQUIRED';
        var eventListenerClassName = mtRequired ? "mojito-ict-string-mt" : textUnits[0]['translationType'] === 'DELTA_OTA' ? "mojito-ict-string-delta-active" : "mojito-ict-string-active";

        if (!node.classList.contains("mojito-ict-string")) {
            try {
                node.className += " mojito-ict-string";
            } catch (e) {
            }

            try {
                var textUnitLKs = textUnits.map(tu => `${tu['repositoryName']}:${tu['assetName']}:${tu['textUnitName']}`).join(";");
                node.setAttribute("data-mojito-textunit-lks", textUnitNames);
            } catch(e) {
            }

            node.addEventListener("mouseenter", (e) => {
                e.target.classList.add(eventListenerClassName);
            });

            node.addEventListener("mouseleave", (e) => {
                e.target.classList.remove(eventListenerClassName);
            });
        }

        if (mtRequired) {
            chrome.runtime.sendMessage(textUnits[0], function (response) {
                if (response.data && node.textContent !== response.data) {
                    if (node.nodeType === Node.ELEMENT_NODE && node.getAttribute('placeholder')) {
                        node.setAttribute('placeholder', response.data)
                    } else {
                        textUnits[0]['isMachineTranslated'] = true
                        node.textContent = response.data;
                        node.classList.add("mojito-mt-ict-string-static");
                        node.addEventListener("click", (e) => addMTCSS(e, node))
                        node.addEventListener("mouseenter", (e) => addMTCSS(e, node))
                        node.addEventListener("mouseleave", (e) => addMTCSS(e, node))
                    }
                }
            });
        }

        node.addEventListener("click", (e) => onClick(e, textUnits));
    }

    installInDOM() {
        var body = document.body || document.getElementsByTagName('body')[0];
        var divIct = document.createElement('div');

        divIct.setAttribute('id', 'mojito-ict');
        // latticehq.com adds forcibly "height: 100%" to the Mojito div, which in turn push the rendering of the lattice
        // app one screen below. Make the Mojito div float to prevent the rendering issue...
        divIct.setAttribute("style", 'float: left;');
        body.insertBefore(divIct, document.body.firstChild);

        ReactDOM.render(
                <IntlProvider locale={this.locale} messages={this.messages}>
                    <IctModalContainer
                        ref={(child) => this.ictModalContainer = child}
                        defaultMojitoBaseUrl={this.mojitoBaseUrl}
                        actionButtons={this.actionButtons}
                        locale={this.locale} />
                </IntlProvider>,
                divIct
                );
    }

    onClickBehavior(e, textUnits) {
        if (e.shiftKey) {
            e.preventDefault();
            e.stopPropagation();
            this.ictModalContainer.showModal(textUnits);
        }
    }

    addMTCSS(e, node) {
        node.classList.add("mojito-mt-ict-string-static");
    }

    processNode(node) {
        var hasMetaData = IctMetadataExtractor.hasMetadata(this.getStringFromNode(node));
        if (hasMetaData) {
            this.wrapNode(node, this.onClickBehavior.bind(this), this.addMTCSS.bind(this));
            this.removeTagsBlockFromNode(node);
        }
    }

    removeTagsBlockFromNode(node) {
        if (this.isElementWithPlaceholder(node)) {
            node.setAttribute('placeholder', TagsBlockDecoder.removeTagsBlock(node.getAttribute('placeholder')));
        } else if (node.textContent) {
            node.textContent = TagsBlockDecoder.removeTagsBlock(node.textContent);
        }
    }

    addMutationObserver() {
        var observer = new MutationObserver((mutationRecords) => {
            mutationRecords.forEach((r) => {
                if (!r.target.classList.contains('mojito-ict')) {
                    this.needsRefresh = true;
                }
            });
        });

        var observerConfig = {childList: true, subtree: true, attribute: true};
        var targetNode = document.body;
        observer.observe(targetNode, observerConfig);
    }

    addScheduledUpdates() {
        this.needsRefresh = true;
        setInterval(() => {
            if (this.needsRefresh) {
                this.needsRefresh = false;
                this.getNodesChildOf(window.document).forEach((node) => {
                    this.processNode(node);
                });
            }
        }, 200);
    }
}

export default Ict;