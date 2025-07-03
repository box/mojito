import React from "react";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

const KeyValueInput = createReactClass({
    displayName: "KeyValueInput",
    propTypes: {
        value: PropTypes.arrayOf(
            PropTypes.shape({
                key: PropTypes.string,
                value: PropTypes.string
            })
        ),
        onChange: PropTypes.func.isRequired,
        inputLabel: PropTypes.string,
        keyLabel: PropTypes.string,
        valueLabel: PropTypes.string
    },

    getDefaultProps() {
        return {
            value: [],
            inputLabel: "Key Value Input",
            keyLabel: "Key",
            valueLabel: "Value"
        };
    },

    handleKeyChange(index, e) {
        const newArr = [...this.props.value];
        newArr[index].key = e.target.value;
        this.props.onChange(newArr);
    },

    handleValueChange(index, e) {
        const newArr = [...this.props.value];
        newArr[index].value = e.target.value;
        this.props.onChange(newArr);
    },

    handleAdd() {
        const newArr = [...this.props.value, { key: "", value: "" }];
        this.props.onChange(newArr);
    },

    handleRemove(index) {
        const newArr = this.props.value.filter((_, i) => i !== index);
        this.props.onChange(newArr);
    },

    render() {
        return (
            <div>
                <label>{this.props.inputLabel}</label>
                <div>
                    {this.props.value.map((pair, index) => (
                        <div key={index} className="key-value-input-row">
                            <input
                                type="text"
                                className="form-control"
                                value={pair.key}
                                onChange={this.handleKeyChange.bind(this, index)}
                                placeholder={this.props.keyLabel}
                            />
                            <input
                                type="text"
                                className="form-control"
                                value={pair.value}
                                onChange={this.handleValueChange.bind(this, index)}
                                placeholder={this.props.valueLabel}
                            />
                            <button
                                className="btn btn-danger"
                                type="button"
                                onClick={() => this.handleRemove(index)}
                            >
                                Remove
                            </button>
                        </div>
                    ))}
                    <button
                        type="button"
                        className="btn btn-default"
                        onClick={this.handleAdd}
                    >
                        + Add Mapping
                    </button>
                </div>
            </div>
        );
    }
});

export default KeyValueInput;
