import React from 'react';

const Spinner = ({ className }) => {
  return <span className={`glyphicon glyphicon-refresh spinning ${className}`}/>
};

export default Spinner;