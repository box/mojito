import React from "react";
import Header from "./header/Header";

class App extends React.Component {
    render() {
        return (

            <div>

                <Header />

                <div className="mll mrl">
                    {this.props.children}
                </div>

            </div>

        );
    }
}

export default App;
