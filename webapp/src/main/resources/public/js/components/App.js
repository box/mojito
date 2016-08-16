import React from "react";
import Router from "react-router";
import Header from "./header/Header";

var App = React.createClass({

    render: function () {
        return (

            <div>

                <Header />

                <div className="mll mrl">
                    {this.props.children}
                </div>

            </div>

        );
    }
});

export default App;
