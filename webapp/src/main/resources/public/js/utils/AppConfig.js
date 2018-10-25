import React, {Children, Component, PropTypes} from 'react';

export class AppConfig extends Component {

    static propTypes = {
        appConfig: PropTypes.object.isRequired,
    }

    static childContextTypes = {
        appConfig: PropTypes.object.isRequired,
    }

    getChildContext() {
        const {appConfig} = this.props
        return {appConfig}
    }

    render() {
        return Children.only(this.props.children)
    }
}

export const withAppConfig = (ComponentToWrap) => {
    return class AppConfigComponent extends Component {

        static contextTypes = {
            appConfig: PropTypes.object.isRequired,
        }

        render() {
            const { appConfig } = this.context
            return (
                <ComponentToWrap {...this.props} appConfig={appConfig} />
            )
        }
    }
}