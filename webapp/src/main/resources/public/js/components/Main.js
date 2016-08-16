import React from "react";
import Router from "react-router";

var Main = React.createClass({

    render: function () {
        return (
          <div>
            {this.props.children}
          </div>
        );
    }
});

export default Main;
