import React from "react";
import PropTypes from "prop-types";
import { DropdownButton, MenuItem } from "react-bootstrap";
import cronstrue from "cronstrue";
import { validateCronExpression } from "../../utils/CronExpressionHelper";

const CUSTOM_CRON_OPTION = "__custom__";

const CRON_EXPRESSION_OPTIONS = [
    { display: "Every 5 Minutes", cron: "0 0/5 * * * ?" },
    { display: "Every Hour", cron: "0 0 * * * ?" },
    { display: "Every Day at Midnight (UTC)", cron: "0 0 0 * * ?" },
    { display: "Custom Cron Expression", cron: CUSTOM_CRON_OPTION },
];

class CronExpressionInput extends React.Component {
    static propTypes = {
        cron: PropTypes.string,
        onChange: PropTypes.func.isRequired,
    };

    constructor(props) {
        super(props);
        const matchedOption = CRON_EXPRESSION_OPTIONS.find(option => option.cron === props.cron);
        this.state = {
            selectedOption: props.cron
                ? (matchedOption ? matchedOption.cron : CUSTOM_CRON_OPTION)
                : null,
            customCron: matchedOption || !props.cron ? "" : (props.cron || "")
        };
    }

    componentDidUpdate(prevProps) {
        if (prevProps.cron !== this.props.cron) {
            const matchedOption = CRON_EXPRESSION_OPTIONS.find(option => option.cron === this.props.cron);
            if (this.state.selectedOption !== CUSTOM_CRON_OPTION || (this.props.cron && matchedOption)) {
                this.setState({
                    selectedOption: this.props.cron
                        ? (matchedOption ? matchedOption.cron : CUSTOM_CRON_OPTION)
                        : null,
                    customCron: matchedOption || !this.props.cron ? "" : (this.props.cron || "")
                });
            }
        }
    }

    handleDropdownSelect = (option) => {
        if (option === CUSTOM_CRON_OPTION) {
            this.setState({ selectedOption: CUSTOM_CRON_OPTION, customCron: "" });
        } else {
            this.setState({ selectedOption: option, customCron: "" });
            this.props.onChange({ target: { name: "cron", value: option } });
        }
    };

    handleCustomCronChange = (e) => {
        const value = e.target.value;
        this.setState({ customCron: value });
        this.props.onChange({ target: { name: "cron", value } });
    };

    getCustomCronDescription = (cron) => {
        if (!cron) { return }
        if (!validateCronExpression(cron)) {
            return <span className="red">Invalid cron expression</span>;
        }
        try {
            return cronstrue.toString(cron);
        } catch (e) {
            return <span className="red">Invalid cron expression</span>;
        }
    };

    render() {
        const { selectedOption, customCron } = this.state;
        let selectedDisplay = "Choose a Cron Expression";
        if (selectedOption === CUSTOM_CRON_OPTION) {
            selectedDisplay = "Custom Cron Expression";
        } else if (selectedOption) {
            selectedDisplay = (CRON_EXPRESSION_OPTIONS.find(option => option.cron === selectedOption) || {}).display;
        }
        return (
            <div>
                <DropdownButton
                    id="cron-expression-dropdown"
                    title={selectedDisplay}
                    onSelect={this.handleDropdownSelect}
                >
                    {CRON_EXPRESSION_OPTIONS.map(option => (
                        <MenuItem
                            key={option.cron}
                            eventKey={option.cron}
                            active={selectedOption === option.cron}
                        >
                            {option.display}
                        </MenuItem>
                    ))}
                </DropdownButton>
                {selectedOption === CUSTOM_CRON_OPTION && (
                    <div>
                        <input
                            type="text"
                            className="form-control mts mbs"
                            placeholder="Enter Custom Cron Expression (e.g. 0 0/5 * * * ?)"
                            value={customCron}
                            onChange={this.handleCustomCronChange}
                            name="cron"
                        />
                        {this.getCustomCronDescription(customCron)}
                    </div>
                )}
            </div>
        );
    }
}

export default CronExpressionInput;