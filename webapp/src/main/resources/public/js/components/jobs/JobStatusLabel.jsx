import React from "react";
import {withAppConfig} from "../../utils/AppConfig";
import PropTypes from "prop-types";

const styleConfig = {
    IN_PROGRESS : {
        "backgroundColor": "rgba(147, 112, 219, 0.24)",
        "color": "#9370DB"
    },
    SUCCEEDED : {
        "backgroundColor": "rgba(85, 151, 69, 0.25)",
        "color": "#559745"
    },
    FAILED : {
        "backgroundColor": "rgba(251, 52, 52, 0.25)",
        "color": "#FB3434FF"
    },
    DISABLED : {
        "backgroundColor": "rgba(182, 182, 182, 0.25)",
        "color": "#656565"
    },
    SCHEDULED : {
        "backgroundColor": "rgb(255, 205, 0, 0.25)",
        "color": "#f7ac11"
    }
}

class JobStatusLabel extends React.Component {

    static propTypes = {
        "status": PropTypes.string.isRequired
    }

    /**
     * @return {XML}
     */
    render() {

        const {status} = this.props;

        return (
            <div className={"job-status"} style={styleConfig[status]}>
                {status && status.replace('_', ' ')}
            </div>
        );
    }
}

export default withAppConfig(JobStatusLabel);
