import React from "react";
import ReactIntl from "react-intl";

let {IntlMixin, FormattedMessage} = ReactIntl;

let RepositoryHeaderColumn = React.createClass({

    mixins: [IntlMixin],

    render: function () {
        return (
            <th className={this.props.className}>{this.props.columnNameMessageId ? this.getIntlMessage(this.props.columnNameMessageId) : ""}</th>
        );
    }
    // TODO: add sort L&F capability.
});
export default RepositoryHeaderColumn;
