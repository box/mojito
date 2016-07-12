import React from "react";
import ReactSidebar from "react-sidebar";

let ReactSidebarResponsive = React.createClass({

    getInitialState() {
        return {"sidebarEnabled": true};
    },

    componentDidMount() {
        let mql = window.matchMedia(`(min-width: 800px)`);
        mql.addListener(this.mediaQueryChanged);
        this.setState({mql: mql});

        this.setState({"sidebarEnabled": mql.matches});

        window.addEventListener('resize', this.onWindowResize);
    },

    componentWillUnmount() {
        this.state.mql.removeListener(this.mediaQueryChanged);

        window.removeEventListener('resize', this.onWindowResize);
    },

    mediaQueryChanged() {
        this.setState({"sidebarEnabled": this.state.mql.matches});
    },

    onWindowResize() {
        // NOTE: resetting the state will trigger a re-render so that the main content
        // width can be calculated correctly since our sidebarWidth is dynamic
        this.refs.sidebar.setState({"sidebarWidth": 0});
    },

    render() {
        return (
            <ReactSidebar ref="sidebar" {...this.props}
                  docked={this.state.sidebarEnabled && this.props.docked} />
        );
    }
});

export default ReactSidebarResponsive;
