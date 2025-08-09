import React, {Children, Component} from 'react';
import PropTypes from 'prop-types';

export class AppConfig extends Component {

    static propTypes = {
        appConfig: PropTypes.object.isRequired,
    }

    static childContextTypes = {
        appConfig: PropTypes.object.isRequired,
        setAppUser: PropTypes.func,
    }

    constructor(props) {
        super(props);
        this.state = { appConfig: props.appConfig };
    }

    getChildContext() {
        return {
            appConfig: this.state.appConfig,
            setAppUser: this.setAppUser,
        }
    }

    setAppUser = (user) => {
        // create a new object reference so consumers re-render
        // also update global APP_CONFIG for callers that read directly (e.g., AuthorityService)
        try { if (window && window.APP_CONFIG) { window.APP_CONFIG.user = user; } } catch (e) {}
        this.setState(prev => ({
            appConfig: { ...prev.appConfig, user }
        }));
    }

    render() {
        return Children.only(this.props.children)
    }
}

export const withAppConfig = (ComponentToWrap) => {
    return class AppConfigComponent extends Component {

        static contextTypes = {
            appConfig: PropTypes.object.isRequired,
            setAppUser: PropTypes.func,
        }

        render() {
            const { appConfig, setAppUser } = this.context
            return (
                <ComponentToWrap {...this.props} appConfig={appConfig} setAppUser={setAppUser} />
            )
        }
    }
}
