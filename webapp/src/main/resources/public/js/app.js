import _ from "lodash";
import React from "react";
import ReactDOM from "react-dom";
import {IndexRoute, Route, Router, useRouterHistory} from "react-router";
import {createHistory} from 'history'
import {Button, Modal} from "react-bootstrap";
import {addLocaleData, FormattedMessage, IntlProvider} from "react-intl";
import {AppConfig} from "./utils/AppConfig";
import App from "./components/App";
import BaseClient from "./sdk/BaseClient";
import Main from "./components/Main";
import Login from "./components/Login";
import AuthCallback from "./components/AuthCallback";
import Workbench from "./components/workbench/Workbench";
import Repositories from "./components/repositories/Repositories";
import BranchesPage from "./components/branches/BranchesPage";
import Drops from "./components/drops/Drops";
import ScreenshotsPage from "./components/screenshots/ScreenshotsPage";
import ScreenshotDropzonePage from "./components/screenshots/ScreenshotDropzonePage";
import UserManagement from "./components/users/UserManagement";
import Settings from "./components/settings/Settings";
import BoxSettings from "./components/settings/BoxSettings";
import DbLatencyMonitoring from "./components/settings/DbLatencyMonitoring";
import WorkbenchActions from "./actions/workbench/WorkbenchActions";
import RepositoryActions from "./actions/RepositoryActions";
import ScreenshotsPageActions from "./actions/screenshots/ScreenshotsPageActions";
import ScreenshotsHistoryStore from "./stores/screenshots/ScreenshotsHistoryStore";
import ScreenshotsRepositoryActions from "./actions/screenshots/ScreenshotsRepositoryActions";

import SearchConstants from "./utils/SearchConstants";
import UrlHelper from "./utils/UrlHelper";
import SearchParamsStore from "./stores/workbench/SearchParamsStore";
import IctMetadataBuilder from "./ict/IctMetadataBuilder";

import LocationHistory from "./utils/LocationHistory";
// NOTE this way of adding locale data is only recommeneded if there are a few locales.
// if there are more, we should load it dynamically using script tags
// https://github.com/yahoo/react-intl/wiki#locale-data-in-browsers
import en from 'react-intl/locale-data/en';
import fr from 'react-intl/locale-data/fr';
import be from 'react-intl/locale-data/be';
import ko from 'react-intl/locale-data/ko';
import ru from 'react-intl/locale-data/ru';
import de from 'react-intl/locale-data/de';
import es from 'react-intl/locale-data/es';
import it from 'react-intl/locale-data/it';
import ja from 'react-intl/locale-data/ja';
import pt from 'react-intl/locale-data/pt';
import zh from 'react-intl/locale-data/zh';
import BranchesPageActions from "./actions/branches/BranchesPageActions";
import BranchesHistoryStore from "./stores/branches/BranchesHistoryStore";
import enMessages from '../../properties/en.properties';
import GoogleAnalytics from "./utils/GoogleAnalytics";
import ShareSearchParamsModalActions from "./actions/workbench/ShareSearchParamsModalActions";
import AuthorityService from "./utils/AuthorityService";
import { isMsalStateless } from './auth/AuthFlags';
import TokenProvider from './auth/TokenProvider';

addLocaleData([...en, ...fr, ...be, ...ko, ...ru, ...de, ...es, ...it, ...ja, ...pt, ...zh]);

__webpack_public_path__ = APP_CONFIG.contextPath + "/";

const browserHistory = useRouterHistory(createHistory)({basename: APP_CONFIG.contextPath});

import(
    /* webpackChunkName: "[request]", webpackMode: "lazy" */
    `../../properties/${APP_CONFIG.locale}.properties`).then(messages => {
    startApp(getMergedMessages(messages));
});


if (APP_CONFIG.googleAnalytics.enabled) {
    let gaUserId = APP_CONFIG.user.username;
    if (APP_CONFIG.googleAnalytics.hashedUserId) {
        gaUserId = GoogleAnalytics.hash(gaUserId);
    }
    GoogleAnalytics.enable(APP_CONFIG.googleAnalytics.trackingId, gaUserId);
    GoogleAnalytics.currentPageView();
}

function getMergedMessages(messages) {
    return messages = _.merge(enMessages, messages);
}

function instrumentMessagesForIct(messages, locale) {

    var localesMap = {
        //TODO: finish
        'fr': 'fr-FR',
        'ko': 'ko-KR',
    };

    locale = _.get(localesMap, locale, locale);

    Object.keys(messages).map((key) => {
        let stack = new Error().stack; // stack is useless here but for tests
        messages[key] = IctMetadataBuilder.getTranslationWithMetadata("mojito", null, key, locale, stack, messages[key], true);
    });
}

function startApp(messages) {

    if (APP_CONFIG.ict) {
        instrumentMessagesForIct(messages, APP_CONFIG.locale);
    }

    ReactDOM.render(
        <AppConfig appConfig={APP_CONFIG}>
            <IntlProvider locale={APP_CONFIG.locale} messages={messages}>
                <Router history={browserHistory}>
                    <Route component={Main}>
                        <Route path="/" component={App}
                               onEnter={onEnterRoot}>
                            <Route path="workbench" component={Workbench}
                                   onEnter={getAllRepositoriesDeffered}
                                   onLeave={onLeaveWorkbench}/>
                            <Route path="repositories" component={Repositories}
                                   onEnter={getAllRepositoriesDeffered}/>
                            <Route path="project-requests" component={Drops}/>
                            <Route path="branches" component={BranchesPage}
                                   onEnter={onEnterBranches}
                                   onLeave={BranchesPageActions.resetBranchesSearchParams}/>
                            <Route path="screenshots" component={ScreenshotDropzonePage}/>
                            <Route path="screenshots-legacy" component={ScreenshotsPage}
                                   onEnter={onEnterScreenshots}
                                   onLeave={ScreenshotsPageActions.resetScreenshotSearchParams}/>
                            <Route path="settings" component={Settings} onEnter={onEnterSettings} onChange={onChangeSettings}>
                                <Route path="user-management" component={UserManagement}/>
                                <Route path="box" component={BoxSettings}/>
                            </Route>
                            <Route path="monitoring" component={DbLatencyMonitoring}/>
                        </Route>
                        <Route path="auth/callback" component={AuthCallback}></Route>
                        <Route path="login" component={Login}></Route>
                    </Route>

                </Router>
            </IntlProvider>
        </AppConfig>
        , document.getElementById("app")
    );

    /**
     * Override handler to customise behavior
     */
    BaseClient.authenticateHandler = function () {
        if (isMsalStateless()) {
            // unlikely to happen but keeping in case
            const pathRelativeToContext = location.pathname.substr(APP_CONFIG.contextPath.length) + window.location.search;
            TokenProvider.login(pathRelativeToContext);
            return;
        }

        let container = document.createElement("div");
        container.setAttribute("id", "unauthenticated-container")
        document.body.appendChild(container);

        function okOnClick() {
            let pathNameStrippedLeadingSlash = location.pathname.substr(1 + APP_CONFIG.contextPath.length, location.pathname.length);
            let currentLocation = pathNameStrippedLeadingSlash + window.location.search;

            if (currentLocation === "") {
                // handles the edge case where the user logs in and doesn't navigate anywhere & then the token expires.
                // Ensures that on relogin the user will be returned to the 'onEnterRoot' view
                window.location.href = UrlHelper.getUrlWithContextPath("/")
            }
            else if (APP_CONFIG.security.unauthRedirectTo) {
                // if we don't log through login page we just reload the current location
                // that needs to be improved eg. when navigating within the workbench if an api calls fails it will
                // re-authenticate and the load the API response instead of showing the frontend
                window.location.href = UrlHelper.getUrlWithContextPath(currentLocation);
            } else {
                // if log through the login page use showPage to redirect to the requested page before auth failed
                window.location.href = UrlHelper.getUrlWithContextPath("/login?") + UrlHelper.toQueryString({"showPage": currentLocation});
            }
        }

        ReactDOM.render(
            <IntlProvider locale={APP_CONFIG.locale} messages={messages}>
                <Modal show={true}>
                    <Modal.Header closeButton={true}>
                        <Modal.Title>
                            <FormattedMessage id="error.modal.header.title" />
                        </Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <FormattedMessage id="error.modal.message.loggedOut" />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button bsStyle="primary" onClick={okOnClick}>
                            <FormattedMessage id="label.okay" />
                        </Button>
                    </Modal.Footer>
                </Modal>
            </IntlProvider>
            , container);
    };

}

/**
 * When leaving the workbench, reset the search param so that when reloading the workbench will start from the
 * default state (avoid flickr and stale data).
 */
function onLeaveWorkbench() {
    setTimeout(() => {
        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL_LOCATION_UPDATE
        });
    }, 1);
}

function getAllRepositoriesDeffered() {
    setTimeout(() => {
        RepositoryActions.getAllRepositories();
    }, 1);
}

function onEnterBranches() {
    setTimeout(() => {
        RepositoryActions.getAllRepositories();
        BranchesPageActions.getBranches();
    }, 1);
}

function onEnterScreenshots() {
    setTimeout(() => {
        ScreenshotsRepositoryActions.getAllRepositories();
    }, 1);
}

function onEnterRoot(nextState, replace) {
    if (location.pathname === '/') {
        replace('/repositories')
    }
}

function onEnterSettings(nextState, replace) {
    onChangeSettings(null, nextState, replace);
}

function onChangeSettings(prevState, nextState, replace) {
    if (location.pathname === '/settings') {
        replace('/settings/user-management')
    }
}

function loadBasedOnLocation(location) {

    if (location.pathname === '/workbench' && location.action === 'POP') {

        if (location.query['link']) {
            // Open the workbench from new deeplinks
            // Uses UPDATE_ALL_LOCATION_UPDATE set in SearchParamStore::onGetSearchParamsSuccess
            let linkUUID = location.query['link'];
            ShareSearchParamsModalActions.getSearchParams(linkUUID);
        } else if (location.state != null) {
            // POP with state is typically back / forward browser buttons
            // Since the store state is restored from the location state, no need to update or push the location
            // on store change, see onSearchParamsStoreChanged()
            const searchParams = location.state.searchParams;
            searchParams["changedParam"] = SearchConstants.UPDATE_ALL_LOCATION_NONE;
            WorkbenchActions.searchParamsChanged(searchParams);
        } else if (location.query != null) {
            // loading for URL with query param. Old deeplink kept for compatiblity
            // Use UPDATE_ALL_LOCATION_UPDATE set in SearchParamsStore.convertQueryToSearchParams
            const searchParams = SearchParamsStore.convertQueryToSearchParams(location.query)
            WorkbenchActions.searchParamsChanged(searchParams);
        }
    }

    if (location.pathname === '/screenshots-legacy' && location.action === 'POP') {
        ScreenshotsHistoryStore.initStoreFromLocationQuery(location.query);
    }

    if (location.pathname === '/branches' && location.action === 'POP') {
        BranchesHistoryStore.initStoreFromLocationQuery(location.query);
    }
}

function onScreenshotsHistoryStoreChange() {
    if (!ScreenshotsHistoryStore.getState().skipLocationHistoryUpdate) {
        LocationHistory.updateLocation(browserHistory, "/screenshots-legacy", ScreenshotsHistoryStore.getQueryParams());
    }
}

ScreenshotsHistoryStore.listen(() => onScreenshotsHistoryStoreChange());


function onBranchesHistoryStoreChange() {
    if (!BranchesHistoryStore.getState().skipLocationHistoryUpdate) {
        LocationHistory.updateLocation(browserHistory, "/branches", BranchesHistoryStore.getQueryParams());
    }
}

BranchesHistoryStore.listen(() => onBranchesHistoryStoreChange());


function onSearchParamsStoreChanged() {
    const workbenchPathname = '/workbench';

    if (window.location.pathname === UrlHelper.getUrlWithContextPath(workbenchPathname)) {
        const searchParamCopy = {...SearchParamsStore.getState()};

        if (searchParamCopy["changedParam"] !== SearchConstants.UPDATE_ALL_LOCATION_NONE) {

            if (searchParamCopy["changedParam"] === SearchConstants.UPDATE_ALL_LOCATION_UPDATE) {
                browserHistory.replace({pathname: workbenchPathname, state: {"searchParams": searchParamCopy}});
            } else {
                browserHistory.push({pathname: workbenchPathname, state: {"searchParams": searchParamCopy}});
            }
        }
    }
}

SearchParamsStore.listen(() => onSearchParamsStoreChanged())

/**
 * Listen to history changes, when doing a POP for the workbench, initialize
 * the SearchParamStore from the query string
 * For the first load the listener is not active, need to access the current
 * location via getCurrentLocation
 */
let currentLocation = browserHistory.getCurrentLocation();

loadBasedOnLocation(currentLocation);

browserHistory.listen(location => {
    loadBasedOnLocation(location);
    GoogleAnalytics.currentPageView();
});
