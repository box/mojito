import React from "react";
import Header from "./header/Header";
import BaseClient from "../sdk/BaseClient";
import { isStateless } from "../auth/AuthFlags";
import TokenProvider from "../auth/TokenProvider";
import { withAppConfig } from "../utils/AppConfig";

class App extends React.Component {

    componentDidMount() {
        window.addEventListener("focus", this.onFocus)
        // Ensure we fetch user immediately on first load in stateless mode
        this.isSessionExpired();
    }

    componentWillUnmount() {
        window.removeEventListener("focus", this.onFocus)
    }

    onFocus = () => {
        this.isSessionExpired();
    }

    isSessionExpired() {
        if (isStateless()) {
            // In stateless mode, MSAL manages token lifetime. Attempt a silent token
            // acquisition; if interaction is required, TokenProvider will trigger
            // a redirect login automatically. Then fetch the current user to update app context.
            TokenProvider.getAccessToken().then(token => {
                if (!token) return;
                fetch(window.location.origin + '/api/users/me', {
                    credentials: 'omit',
                    headers: { 'Authorization': `Bearer ${token}` }
                }).then(r => r.ok ? r.json() : null)
                  .then(user => {
                      if (user && this.props.setAppUser) {
                          this.props.setAppUser(user);
                      }
                  })
                  .catch(() => {});
            });
            return;
        }

        fetch(window.location.origin + '/api/users/session', {
            credentials: 'include',
            redirect: "manual"
        }).then(response => {
            if (response.status !== 200) {
                BaseClient.authenticateHandler();
            }
        });
    }

    render() {
        return (

            <div>

                <Header />

                <div className="mll mrl">
                    {this.props.children}
                </div>

            </div>

        );
    }
}

export default withAppConfig(App);
