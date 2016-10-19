import React from "react";
import {Table, ProgressBar, Button, Label} from "react-bootstrap";
import {History, Link} from "react-router";
import {FormattedMessage, FormattedNumber} from "react-intl";
import Locales from "../../utils/Locales";
import RepositoryStore from "../../stores/RepositoryStore";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchConstants from "../../utils/SearchConstants";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore.js";

let DropDetail = React.createClass({
    getInitialState() {
        return {};
    },

    propTypes: {
        /** @type {Drop} */
        "drop": React.PropTypes.object.isRequired,

        /** @type {function} */
        "onCloseRequest": React.PropTypes.func.isRequired
    },

    /**
     * Handle onClick close button
     */
    onClickClose() {
        if (this.props.onCloseRequest) {
            this.props.onCloseRequest();
        }
    },

    /**
     * Update the Workbench search params to load strings that needs to be translated for a given locale
     *
     * @param {string} bcp47Tag
     */
    updateSearchParamsForLocale(bcp47Tag) {

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": [this.props.repoId],
            "bcp47Tags": [bcp47Tag],
            "status": SearchParamsStore.STATUS.ALL
        });
    },

    render: function () {
        let translationKits = Locales.sortByDisplayName(this.props.drop.translationKits, translationKit => translationKit.locale.bcp47Tag);

        let rows = translationKits.map(tk => {
            return <tr>
                <td>
                    <div>
                        <Link onClick={this.updateSearchParamsForLocale.bind(this, tk.locale.bcp47Tag)}
                              to='/workbench'>{tk.localeDisplayName}</Link>
                    </div>
                </td>
                <td><FormattedNumber value={tk.wordCount}/></td>
            </tr>;
        });

        return (
                <div>
                    <div className="title">
                        <Button className="close" onClick={this.onClickClose}>×</Button>
                    </div>
                    <div className="side-bar-content-container">
                        <Table className="repo-stats-table">
                            <thead>
                            <tr>
                                <th><FormattedMessage id="drops.tableHeader.locales" /></th>
                                <th><FormattedMessage id="drops.tableHeader.wordCount" /></th>
                            </tr>
                            </thead>
                            <tbody>
                            {rows}
                            </tbody>
                        </Table>
                    </div>
                </div>

        );
    }

});

export default DropDetail;
