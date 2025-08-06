import React from "react";
import Header from "./header/Header";
import BaseClient from "../sdk/BaseClient";
import { isStateless } from "../auth/AuthFlags";
import TokenProvider from "../auth/TokenProvider";

class App extends React.Component {

    componentDidMount() {
        window.addEventListener("focus", this.onFocus)
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
            // a redirect login automatically.
            TokenProvider.getAccessToken();
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

export default App;
