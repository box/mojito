import $ from "jquery";
import React from "react";
import ReactDOM from "react-dom";
import {Router, Route, IndexRoute} from "react-router";
import {createHistory} from "history";
import {IntlProvider, addLocaleData} from "react-intl";
import App from "./components/App";
import BaseClient from "./sdk/BaseClient";
import Main from "./components/Main";
import Login from "./components/Login";
import Workbench from "./components/workbench/Workbench";
import Repositories from "./components/repositories/Repositories";
import Drops from "./components/drops/Drops";
import Settings from "./components/settings/Settings";
import WorkbenchActions from "./actions/workbench/WorkbenchActions";
import SearchConstants from "./utils/SearchConstants";
import SearchParamsStore from "./stores/workbench/SearchParamsStore";

// NOTE this way of adding locale data is only recommeneded if there are a few locales.
// if there are more, we should generate individual bundle js.
// https://github.com/yahoo/react-intl/wiki#locale-data-in-browsers
import en from 'react-intl/locale-data/en';
import fr from 'react-intl/locale-data/fr';
import be from 'react-intl/locale-data/be';
import ko from 'react-intl/locale-data/ko';
addLocaleData([...en, ...fr, ...be, ...ko]);

let history = createHistory();

ReactDOM.render(
    <IntlProvider locale={LOCALE} messages={MESSAGES}>
        <Router history={history}>
            <Route component={Main}>
                <Route path="/" component={App}>
                    <Route path="workbench" component={Workbench} onLeave={onLeaveWorkbench}/>
                    <Route path="repositories" component={Repositories}/>
                    <Route path="project-requests" component={Drops}/>
                    <Route path="settings" component={Settings}/>
                    <IndexRoute component={Repositories}/>
                </Route>
                <Route path="login" component={Login}></Route>
            </Route>

        </Router>
    </IntlProvider>
    , document.getElementById("app")
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

