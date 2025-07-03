import React from "react";
import PropTypes from "prop-types";
import { ThirdPartySyncAction } from "../../utils/ThirdPartySyncAction";
import { OverlayTrigger, Tooltip } from "react-bootstrap";


const ThirdPartySyncActionsInput = ({ selectedActions, onChange }) => {
    const actionKeys = Object.keys(ThirdPartySyncAction);

    const handleCheckboxChange = (e) => {
        const { name, checked } = e.target;
        let updated;
        if (checked) {
            updated = [...selectedActions, name];
        } else {
            updated = selectedActions.filter(a => a !== name);
        }
        onChange(updated);
    };

    return (
        <div className="form-group mbm">
            <label>Enable Sync Actions*</label>
            <div>
                {actionKeys.map(key => (
                    <div className="form-check-control" key={key}>
                        <input
                            className="form-check-input"
                            type="checkbox"
                            id={`action-checkbox-${key}`}
                            name={key}
                            checked={selectedActions.includes(key)}
                            onChange={handleCheckboxChange}
                        />
                        <label className="form-check-label mls" htmlFor={`action-checkbox-${key}`}>
                            {ThirdPartySyncAction[key].displayName}
                        </label>
                        {ThirdPartySyncAction[key].tooltip && (
                            <OverlayTrigger
                                placement="right"
                                overlay={<Tooltip id={`action-tooltip-${key}`}>{ThirdPartySyncAction[key].tooltip}</Tooltip>}
                            >
                                <span className="textunit-gitInfo glyphicon glyphicon-info-sign mls"/>
                            </OverlayTrigger>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
};

ThirdPartySyncActionsInput.propTypes = {
    selectedActions: PropTypes.arrayOf(PropTypes.string).isRequired,
    onChange: PropTypes.func.isRequired,
};

export default ThirdPartySyncActionsInput;