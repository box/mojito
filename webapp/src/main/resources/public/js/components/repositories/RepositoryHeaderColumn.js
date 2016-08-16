import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";

let RepositoryHeaderColumn = React.createClass({

    render: function () {
        return (
            <th className={this.props.className}>{this.props.columnNameMessageId ? this.props.intl.formatMessage({ id: this.props.columnNameMessageId }) : ""}</th>
        );
    }
    // TODO: add sort L&F capability.
});
export default injectIntl(RepositoryHeaderColumn);
