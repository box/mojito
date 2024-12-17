import React from "react";
import Router from "react-router";

class Main extends React.Component {
  render() {
      return (
        <div>
          {this.props.children}
        </div>
      );
  }
}

export default Main;
