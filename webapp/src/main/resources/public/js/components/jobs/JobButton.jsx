import React from "react";
import JobActions from "../../actions/jobs/JobActions";
import PropTypes from "prop-types";


class JobButton extends React.Component {

    static propTypes = {
        "job": PropTypes.object.isRequired,
        "type": PropTypes.string.isRequired,
    }

    constructor(props) {
        super(props);
        this.timeout = null;

        this.state = {
            disabled: false
        }
    }

    handleClick = (job, type, button) => {
        // Disable the button to avoid register spam clicks
        this.setState({disabled: true})

        switch(type) {
            case JobButton.TYPES.RUN:
                JobActions.triggerJob(job);
                break;
            case JobButton.TYPES.DISABLE:
                JobActions.disableJob(job);
                break;
            case JobButton.TYPES.ENABLE:
                JobActions.enableJob(job);
                break;
        }

        this.timeout = setTimeout(() => {
            // Wait before switching on the button to avoid double clicks
            this.setState({disabled: false})
        }, 500)
    }

    componentWillUnmount() {
        if(this.timeout) {
            clearTimeout(this.timeout);
        }
    }

    /**
     * @return {XML}
     */
    render() {

        const {type, disabled, job} = this.props;

        return (
            <button className={`job-button ${disabled || this.state.disabled ? 'disabled' : ''}`} disabled={disabled || this.state.disabled}
                    onClick={() => this.handleClick(job, type)} >
                {type}
            </button>
        );
    }
}

JobButton.TYPES = {
    "RUN": "RUN",
    "DISABLE": "DISABLE",
    "ENABLE": "ENABLE"
}

export default JobButton;
