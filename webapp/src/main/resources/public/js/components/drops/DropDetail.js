import PropTypes from 'prop-types';
import React from "react";
import {Table, ProgressBar, Button, Label} from "react-bootstrap";
import {History, Link} from "react-router";
import {FormattedMessage, FormattedNumber} from "react-intl";
import Locales from "../../utils/Locales";
import RepositoryStore from "../../stores/RepositoryStore";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchConstants from "../../utils/SearchConstants";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore.js";

class DropDetail extends React.Component {
    static propTypes = {
        /** @type {Drop} */
        "drop": PropTypes.object.isRequired,

    };

    state = {};

    /**
     * Update the Workbench search params to load strings that needs to be translated for a given locale
     *
     * @param {string} bcp47Tag
     */
    updateSearchParamsForLocale = (repoId, bcp47Tag) => {
        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": [repoId],
            "bcp47Tags": [bcp47Tag],
            "status": SearchParamsStore.STATUS.ALL
        });
    };

    render() {
        let translationKits = Locales.sortByDisplayName(this.props.drop.translationKits, translationKit => translationKit.locale.bcp47Tag);

        let rows = translationKits.map(tk => {
            let importLabel = tk.imported ?
                (
                    <Label bsStyle="success" className="mrs">
                        <FormattedMessage id="drops.table.row.imported"/>
                    </Label>
                ) : "";

            return <tr key={"DropDetail.row." + tk.localeDisplayName}>
                <td>
                    <div>
                        <Link onClick={this.updateSearchParamsForLocale.bind(this, this.props.drop.repository.id, tk.locale.bcp47Tag)}
                              to='/workbench'>{tk.localeDisplayName}</Link>
                    </div>
                </td>
                <td>{importLabel}</td>
                <td><FormattedNumber value={tk.wordCount}/></td>
            </tr>;
        });

        return (
                <div>
                    <div className="side-bar-content-container">
                        <Table className="repo-stats-table">
                            <thead>
                            <tr>
                                <th><FormattedMessage id="drops.tableHeader.locales" /></th>
                                <th></th>
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
}

export default DropDetail;
