import React from 'react';
import TokenProvider from '../auth/TokenProvider';
import UrlHelper from '../utils/UrlHelper';

class AuthCallback extends React.Component {
    componentDidMount() {
        TokenProvider.handleRedirect().then(result => {
            let returnTo = '/repositories';
            try {
                if (result && result.state) {
                    const parsed = JSON.parse(result.state);
                    if (parsed && parsed.returnTo) returnTo = parsed.returnTo;
                }
            } catch (e) { /* ignore */ }

            if (!returnTo || typeof returnTo !== 'string') returnTo = '/repositories';
            window.location.replace(UrlHelper.getUrlWithContextPath(returnTo));
        });
    }

    render() {
        return (
            <div className="container ptl pbl mtl mbl">
                <h4>Signing you in…</h4>
            </div>
        );
    }
}

export default AuthCallback;

