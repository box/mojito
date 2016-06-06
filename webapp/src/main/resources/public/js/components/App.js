import React from "react";
import Router from "react-router";
import Header from "./header/Header";
import ReactIntl from 'react-intl';

var {IntlMixin} = ReactIntl;

var App = React.createClass({

    mixins: [IntlMixin],

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
