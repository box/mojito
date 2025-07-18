import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, FormControl} from "react-bootstrap";
import UrlHelper from "../utils/UrlHelper";
import {withAppConfig} from "../utils/AppConfig";
import loginLogoPng from "../../img/logo-login.png";

class Login extends React.Component {
    getLogoutElement = () => {
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
    };

    getErrorElement = () => {
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
    };

    /**
     * @param {string} showPage
     * @return {string}
     */
    getLoginFormPostUrl = (showPage) => {
        let result = UrlHelper.getUrlWithContextPath('/login');

        if (showPage) {
            result += '?' + UrlHelper.toQueryString({"showPage": showPage});
        }

        return result;
    };

    renderOAuth = () => {
        const registrations = Object.entries(this.props.appConfig.security.oAuth2);
        return <div>
            {
                registrations.length > 0 &&
                <div className="mtxl mbl">
                    <h4 className="text-center color-gray-light">
                        <FormattedMessage id="login.form.oauth"/>
                    </h4>
                </div>
            }
            {registrations.map(([registrationId, registration]) => this.renderOAuthRegistration(registrationId, registration))}
        </div>;
    }

    renderOAuthRegistration = (registrationId, registration) => {
        return <div className="mts">
            <Button bsClass="form-control btn-secondary btn-block"
                    onClick={() => {
                        window.location.href = `/login/oauth2/authorization/${registrationId}`
                    }}>
                {registration.uiLabelText}
            </Button>
        </div>;
    }

    render() {
        let {logout, error, showPage} = this.props.location.query;

        var logoutElement = this.getLogoutElement();
        var errorElement = this.getErrorElement();
        var loginFormPostUrl = this.getLoginFormPostUrl(showPage);

        return (
            <div className="container login-container">
                <div className="row">
                    <div className="center-block login-logo-container">
                        <img src={loginLogoPng} className="logo mbm" alt="Mojito"/>
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
                                             placeholder={this.props.intl.formatMessage({id: "login.form.username"})}/>
                            </div>
                            <div className="form-group pbs pts">
                                <FormControl className="form-control" type="password" name="password"
                                             placeholder={this.props.intl.formatMessage({id: "login.form.password"})}/>
                            </div>
                            <div className="pbs ptl">
                                <Button type="submit" bsClass="form-control btn-primary btn-block">
                                    <FormattedMessage id="login.form.login"/>
                                </Button>
                            </div>

                            <FormControl type="hidden" name="_csrf" value={this.props.appConfig.csrfToken}/>
                        </form>

                        {this.renderOAuth()}
                    </div>
                </div>
            </div>
        );
    }
}

export default withAppConfig(injectIntl(Login));
