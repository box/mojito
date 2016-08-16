import $ from "jquery";
import React from "react";
import {FormattedMessage, FormattedNumber, injectIntl} from "react-intl";
import {FormControl, Button} from "react-bootstrap";

let Login = React.createClass({

    getLogoutElement: function () {
        let {logout} = this.props.location.query;

        var logoutElement;
        if (typeof logout != 'undefined') {
            logoutElement = (<div className="form-group">
                <p className="text-center color-info">
                    <FormattedMessage id="login.form.logout"/>
                </p>
            </div>);
        }

        return logoutElement;
    },

    getErrorElement: function () {
        let {error} = this.props.location.query;

        var errorElement;
        if (typeof error != 'undefined') {
            errorElement = (
                <div className="form-group">
                    <p className="text-center color-info">
                        <FormattedMessage id="login.form.error"/>
                    </p>
                </div>
            );
        }

        return errorElement;
    },

    /**
     * @param {string} showPage
     * @return {string}
     */
    getLoginFormPostUrl: function (showPage) {
        let result = '/login';

        if (showPage) {
            result += '?' + $.param({"showPage": showPage});
        }

        return result;
    },

    render: function () {
        let {logout, error, showPage} = this.props.location.query;

        var logoutElement = this.getLogoutElement();
        var errorElement = this.getErrorElement();
        var loginFormPostUrl = this.getLoginFormPostUrl(showPage);

        return (
            <div className="container login-container">
                <div className="row">
                    <div className="center-block login-logo-container">
                        <img src="/img/logo-login.png" className="logo mbm" alt="Mojito"/>
                    </div>
                    <div className="center-block login-form-container">
                        <form action={loginFormPostUrl} method="post">
                            {errorElement}
                            {logoutElement}
                            <div className="form-group pbs pts">
                                <h4 className="text-center color-gray-light">
                                    <FormattedMessage id="login.form.title"/>
                                </h4>
                            </div>
                            <div className="form-group pbs pts">
                                <FormControl className="form-control" type="text" name="username"
                                             placeholder={this.props.intl.formatMessage({ id: "login.form.username" })}/>
                            </div>
                            <div className="form-group pbs pts">
                                <FormControl className="form-control" type="password" name="password"
                                             placeholder={this.props.intl.formatMessage({ id: "login.form.password" })}/>
                            </div>
                            <div className="form-group pbs ptl">
                                <Button type="submit" bsClass="form-control col-md-3 btn-primary btn-block">
                                    <FormattedMessage id="login.form.login"/>
                                </Button>
                            </div>
                            <FormControl type="hidden" name="_csrf" value={CSRF_TOKEN}/>
                        </form>
                    </div>
                </div>
            </div>
        );
    }
});

export default injectIntl(Login);
