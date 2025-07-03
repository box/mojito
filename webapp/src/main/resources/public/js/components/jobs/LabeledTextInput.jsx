import React from "react";

const LabeledTextInput = ({ label, placeholder, inputName, value, onChange }) => (
    <div className="form-group mbm">
        <label>{label}</label>
        <input
            className="form-control"
            type="text"
            name={inputName}
            placeholder={placeholder}
            value={value}
            onChange={onChange}
        />
    </div>
);

export default LabeledTextInput;
