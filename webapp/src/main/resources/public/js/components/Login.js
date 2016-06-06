import $ from "jquery";
import React from "react";
import Router from "react-router";
import ReactIntl from 'react-intl';

import {Input, ButtonInput} from 'react-bootstrap';

let {IntlMixin, FormattedMessage, FormattedNumber} = ReactIntl;

let Login = React.createClass({

    mixins: [IntlMixin],

    getLogoutElement: function () {
        let { logout } = this.props.location.query;

        var logoutElement;
        if (typeof logout != 'undefined') {
            logoutElement = (<div className="form-group">
                <p className="text-center color-info">
                    <FormattedMessage message={this.getIntlMessage("login.form.logout")} />
                </p>
            </div>);
        }

        return logoutElement;
    },

    getErrorElement: function () {
        let { error } = this.props.location.query;

        var errorElement;
        if (typeof error != 'undefined') {
            errorElement = (
                <div className="form-group">
                    <p className="text-center color-info">
                        <FormattedMessage message={this.getIntlMessage("login.form.error")} />
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
            result += '?' + $.param({ "showPage": showPage });
        }

        return result;
    },

    render: function () {
        let { logout, error, showPage } = this.props.location.query;

        var logoutElement = this.getLogoutElement();
        var errorElement = this.getErrorElement();
        var loginFormPostUrl = this.getLoginFormPostUrl(showPage);

        return (
            <div className="container login-container">
                <div className="row">
                    <div className="center-block login-logo-container">
                        <img src="/img/logo-391x90.png" className="logo" alt="Box Mojito" />
                    </div>
                    <div className="center-block login-form-container">
                        <form action={loginFormPostUrl} method="post">
                            {errorElement}
                            {logoutElement}
                            <div className="form-group pbs pts">
                                <h4 className="text-center color-gray-light">
                                    <FormattedMessage message={this.getIntlMessage("login.form.title")} />
                                </h4>
                            </div>
                            <div className="form-group pbs pts">
                                <Input className="form-control" type="text" name="username" placeholder={this.getIntlMessage("login.form.username")}/>
                            </div>
                            <div className="form-group pbs pts">
                                <Input className="form-control" type="password" name="password" placeholder={this.getIntlMessage("login.form.password")}/>
                            </div>
                            <div className="form-group pbs ptl">
                                <ButtonInput type="submit" value={this.getIntlMessage("login.form.login")} className="form-control btn btn-default col-md-3 btn-primary btn-block"/>
                            </div>
                            <Input type="hidden" name="_csrf" value={CSRF_TOKEN}/>
                        </form>
                    </div>
                </div>
            </div>
        );
    }
});

export default Login;
