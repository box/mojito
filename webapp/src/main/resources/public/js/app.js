import $ from "jquery";
import _ from "lodash";

import React from "react";
import {Router, Route, IndexRoute} from "react-router";
import { createHistory } from "history";

import App from "./components/App";
import BaseClient from "./sdk/BaseClient";
import Main from "./components/Main";
import Login from "./components/Login";
import Workbench from "./components/workbench/Workbench";
import Repositories from "./components/repositories/Repositories";
import Drops from "./components/drops/Drops";
import Settings from "./components/settings/Settings";

import RepositoryActions from "./actions/RepositoryActions";
import WorkbenchActions from "./actions/workbench/WorkbenchActions";
import SearchConstants from "./utils/SearchConstants";
import SearchParamsStore from "./stores/workbench/SearchParamsStore";

let history = createHistory();

function createElement(Component, props) {
    // NOTE: Passing the props down like this introduces the following warning:
    // Warning: owner-based and parent-based contexts differ (values: `undefined` vs `en`) for key (locales) while mounting Workbench (see: http://fb.me/react-context-by-parent)
    // warning.js:49 Warning: owner-based and parent-based contexts differ (values: `undefined` vs `[object Object]`) for key (messages) while mounting Workbench (see: http://fb.me/react-context-by-parent)
    // https://gist.github.com/jimfb/0eb6e61f300a8c1b2ce7
    return <Component {...props} locales={LOCALE} messages={MESSAGES} />;
}

React.render(
    <Router history={history} createElement={createElement}>
        <Route component={Main}>
            <Route path="/" component={App}>
                <Route path="workbench" component={Workbench} onLeave={onLeaveWorkbench} />
                <Route path="repositories" component={Repositories} />
                <Route path="project-requests" component={Drops} />
                <Route path="settings" component={Settings} />
                <IndexRoute component={Repositories} />
            </Route>
            <Route path="login" component={Login}></Route>
        </Route>

    </Router>, document.getElementById("app")
);


/**
 * When leaving the workbench, reset the search param so that when reloading the workbench will start from the
 * default state (avoid flickr and stale data).
 */
function onLeaveWorkbench() {
    WorkbenchActions.searchParamsChanged({
        "changedParam": SearchConstants.UPDATE_ALL
    });
}

/**
 * Listen to history changes, when doing a POP for the workbench, initialize the SearchParamStore from the query string
 */
history.listen(location => {
    if (location.pathname === '/workbench' && location.action === 'POP') {
        WorkbenchActions.searchParamsChanged(SearchParamsStore.convertQueryToSearchParams(location.query));
    }
});

/**
 * Override handler to customise behavior
 */
BaseClient.authenticateHandler = function () {
    alert('Session expired.  Please re-authenticate.');

    let pathNameStrippedLeadingSlash = location.pathname.substr(1, location.pathname.length);
    let currentLocation = pathNameStrippedLeadingSlash + window.location.search;

    window.location.href = "/login?" + $.param({"showPage": currentLocation});
};

