import keycode from "keycode";

import React from "react";
import {withRouter} from 'react-router';

import AltContainer from "alt-container";

import SearchConstants from "../../utils/SearchConstants";

import ScreenshotsRepositoryStore from "../../stores/screenshots/ScreenshotsRepositoryStore";
import ScreenshotsLocaleStore from "../../stores/screenshots/ScreenshotsLocaleStore";
import ScreenshotsPageStore from "../../stores/screenshots/ScreenshotsPageStore";
import ScreenshotsSearchTextStore from "../../stores/screenshots/ScreenshotsSearchTextStore";
import ScreenshotsPaginatorStore from "../../stores/screenshots/ScreenshotsPaginatorStore";
import ScreenshotsReviewModalStore from "../../stores/screenshots/ScreenshotsReviewModalStore";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";

import ScreenshotsPageActions from "../../actions/screenshots/ScreenshotsPageActions";
import ScreenshotsRepositoryActions from "../../actions/screenshots/ScreenshotsRepositoryActions";
import ScreenshotsLocaleActions from "../../actions/screenshots/ScreenshotsLocaleActions";
import ScreenshotsSearchTextActions from "../../actions/screenshots/ScreenshotsSearchTextActions";
import ScreenshotsPaginatorActions from "../../actions/screenshots/ScreenshotsPaginatorActions";
import ScreenshotActions from "../../actions/screenshots/ScreenshotActions";
import ScreenshotsReviewModalActions from "../../actions/screenshots/ScreenshotsReviewModalActions";
import ScreenshotsHistoryActions from "../../actions/screenshots/ScreenshotsHistoryActions";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";

import RepositoryDropdown from "./RepositoryDropdown";
import LocalesDropdown from "./LocalesDropdown";
import Paginator from "./Paginator";
import ScreenshotsSearchText from "./ScreenshotsSearchText";
import ScreenshotsGrid from "./ScreenshotsGrid";
import StatusDropdown from "./StatusDropdown";
import ScreenshotReviewModal from "./ScreenshotReviewModal";

class ScreenshotsPage extends React.Component {

    componentDidMount() {
        this.addWindowKeyUpDownListener();
    }

    componentWillUnmount() {
        this.removeWindowKeyDownEventListener();
    }

    /**
     * Don't update the component has it has no rendering based on props or
     * state. Avoid useless re-rendering when location changes due to updates
     * from onScreenshotsHistoryStoreChange().
     */
    shouldComponentUpdate() {
        return false;
    }

    addWindowKeyUpDownListener() {
        this.keydownEventListener = this.onWindowKeyDown.bind(this)
        window.addEventListener('keydown', this.keydownEventListener);
    }

    removeWindowKeyDownEventListener() {
        window.removeEventListener('keydown', this.keydownEventListener);
    }

    /**
     * Handle keyboard event to allow screenshots navigation
     *
     * @param {SynteticEvent} e
     * @returns {undefined}
     */
    onWindowKeyDown(e) {
        switch (keycode(e)) {
            case "left":
                if (!ScreenshotsPageStore.getState().searching) {
                    this.goToPreviousScreenshot();
                }
                break;
            case "right":
                if (!ScreenshotsPageStore.getState().searching) {
                    this.goToNextScreenshot();
                }
                break;
        }
    }

    onScreenshotClicked(idx) {
        if (ScreenshotsPageStore.getState().selectedScreenshotIdx !== idx) {
            ScreenshotsPageActions.changeSelectedScreenshotIdx(idx);
        }
    }

    onScreenshotsTextUnitNameClick(e, textUnit) {
        e.stopPropagation();

        var selectedRepositoryIds = ScreenshotsRepositoryStore.getState().selectedRepositoryIds;

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": selectedRepositoryIds,
            "searchText": textUnit.name,
            "searchAttribute": SearchParamsStore.SEARCH_ATTRIBUTES.STRING_ID,
            "searchType": SearchParamsStore.SEARCH_TYPES.EXACT,
            "bcp47Tags": ScreenshotsRepositoryStore.getAllBcp47TagsForRepositoryIds(selectedRepositoryIds),
        });

        this.props.router.push("/workbench", null, null);
    }

    onScreenshotsTextUnitTargetClick(e, textUnit, locale) {
        e.stopPropagation();

        var searchText =  textUnit.target ? textUnit.target : textUnit.renderedTarget;

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": ScreenshotsRepositoryStore.getState().selectedRepositoryIds,
            "searchText": searchText,
            "searchAttribute": SearchParamsStore.SEARCH_ATTRIBUTES.TARGET,
            "searchType": SearchParamsStore.SEARCH_TYPES.EXACT,
            "bcp47Tags": [locale],
        });

        this.props.router.push("/workbench", null, null);
    }

    goToPreviousScreenshot() {

        let selectedScreenshotIdx = ScreenshotsPageStore.getState().selectedScreenshotIdx;
        let screenshotsPaginatorStoreState = ScreenshotsPaginatorStore.getState();

        if (selectedScreenshotIdx === 0) {
            if (screenshotsPaginatorStoreState.currentPageNumber > 1) {
                ScreenshotsHistoryActions.disableHistoryUpdate();
                ScreenshotsPaginatorActions.goToPreviousPage();
                ScreenshotsPageActions.changeSelectedScreenshotIdx(screenshotsPaginatorStoreState.limit - 1);
                ScreenshotsHistoryActions.enableHistoryUpdate();
                ScreenshotsPageActions.performSearch();
            }
        } else {
            ScreenshotsPageActions.changeSelectedScreenshotIdx(selectedScreenshotIdx - 1);
        }
    }

    goToNextScreenshot() {
        let screenshotsPaginatorStoreState = ScreenshotsPaginatorStore.getState();
        let screenshotsPageStoreState = ScreenshotsPageStore.getState();
        let selectedScreenshotIdx = ScreenshotsPageStore.getState().selectedScreenshotIdx;

        if (selectedScreenshotIdx === (screenshotsPaginatorStoreState.limit - 1)) {
            ScreenshotsHistoryActions.disableHistoryUpdate();
            ScreenshotsPaginatorActions.goToNextPage();
            ScreenshotsPageActions.changeSelectedScreenshotIdx(0);
            ScreenshotsHistoryActions.enableHistoryUpdate();
            ScreenshotsPageActions.performSearch();
        } else if (selectedScreenshotIdx < screenshotsPageStoreState.screenshotsData.length - 1) {
            ScreenshotsPageActions.changeSelectedScreenshotIdx(selectedScreenshotIdx + 1);
        }
    }

    render() {

        return (
                <div>
                    <div>
                        <div className="pull-left">
                            <AltContainer store={ScreenshotsRepositoryStore}>
                                <RepositoryDropdown
                                    onSelectedRepositoryIdsChanged={(selectedRepositoryIds) => {
                                        ScreenshotsHistoryActions.disableHistoryUpdate();
                                        ScreenshotsPaginatorActions.changeCurrentPageNumber(1);
                                        ScreenshotsRepositoryActions.changeSelectedRepositoryIds(selectedRepositoryIds);
                                        ScreenshotsHistoryActions.enableHistoryUpdate();
                                        ScreenshotsPageActions.performSearch();
                                        }}
                                    onDropdownToggle={ScreenshotsRepositoryActions.changeDropdownOpen}/>
                            </AltContainer>
                            <AltContainer store={ScreenshotsLocaleStore}>
                                <LocalesDropdown
                                    onSelectedBcp47TagsChanged={(selectedBcp47Tags) => {
                                        ScreenshotsHistoryActions.disableHistoryUpdate();
                                        ScreenshotsPaginatorActions.changeCurrentPageNumber(1);
                                        ScreenshotsLocaleActions.changeSelectedBcp47Tags(selectedBcp47Tags);
                                        ScreenshotsHistoryActions.enableHistoryUpdate();
                                        ScreenshotsPageActions.performSearch();
                                        }}
                                    onDropdownToggle={ScreenshotsLocaleActions.changeDropdownOpen}/>
                            </AltContainer>
                        </div>

                        <AltContainer store={ScreenshotsSearchTextStore}>
                            <ScreenshotsSearchText
                                onSearchAttributeChanged={(attribute) => {
                                    ScreenshotsHistoryActions.disableHistoryUpdate();
                                    ScreenshotsPaginatorActions.changeCurrentPageNumber(1);
                                    ScreenshotsSearchTextActions.changeSearchAttribute(attribute);
                                    ScreenshotsHistoryActions.enableHistoryUpdate();
                                    ScreenshotsPageActions.performSearch();
                                    }}
                                onSearchTypeChanged={(screenshotRunType) => {
                                    ScreenshotsHistoryActions.disableHistoryUpdate();
                                    ScreenshotsPaginatorActions.changeCurrentPageNumber(1);
                                    ScreenshotsSearchTextActions.changeSearchType(type);
                                    ScreenshotsHistoryActions.enableHistoryUpdate();
                                    ScreenshotsPageActions.performSearch();
                                    }}
                                onSearchTextChanged={
                                    (text) => {
                                        // we don't want to update the history for each key stroke
                                        // update  the history when performing the search
                                        ScreenshotsHistoryActions.disableHistoryUpdate();
                                        ScreenshotsSearchTextActions.changeSearchText(text);
                                    }}
                                onPerformSearch={() => {
                                    ScreenshotsPaginatorActions.changeCurrentPageNumber(1);
                                    ScreenshotsHistoryActions.enableHistoryUpdate();
                                    ScreenshotsPageActions.performSearch();
                                }}
                                />
                        </AltContainer>

                        <AltContainer store={ScreenshotsSearchTextStore}
                                      shouldComponentUpdate={(props, nextProps, nextState) => {
                                //TODO investigate that pattern vs dedicated store
                                return props.status !== nextState.status ||
                                       props.screenshotRunType !== nextState.screenshotRunType
                                      }} >
                            <StatusDropdown onStatusChanged={(statusAndIdx) => {
                                    ScreenshotsHistoryActions.disableHistoryUpdate();
                                    ScreenshotsPaginatorActions.changeCurrentPageNumber(1);
                                    ScreenshotsSearchTextActions.changeStatus(statusAndIdx);
                                    ScreenshotsHistoryActions.enableHistoryUpdate();
                                    ScreenshotsPageActions.performSearch();
                                            }}

                                    onScreenshotRunTypeChanged={(screenshotRunType) => {
                                        ScreenshotsHistoryActions.disableHistoryUpdate();
                                        ScreenshotsPaginatorActions.changeCurrentPageNumber(1);
                                        ScreenshotsSearchTextActions.changeScreenshotRunType(screenshotRunType);
                                        ScreenshotsHistoryActions.enableHistoryUpdate();
                                        ScreenshotsPageActions.performSearch();
                                    }}/>
                        </AltContainer>

                        <AltContainer store={ScreenshotsPaginatorStore}>
                            <Paginator
                                onPreviousPageClicked={() => {
                                        ScreenshotsHistoryActions.disableHistoryUpdate();
                                        ScreenshotsPaginatorActions.goToPreviousPage();
                                        ScreenshotsPageActions.changeSelectedScreenshotIdx(0);
                                        ScreenshotsHistoryActions.enableHistoryUpdate();
                                        ScreenshotsPageActions.performSearch();
                                }}
                                onNextPageClicked={() => {
                                            ScreenshotsHistoryActions.disableHistoryUpdate();
                                            ScreenshotsPaginatorActions.goToNextPage();
                                            ScreenshotsPageActions.changeSelectedScreenshotIdx(0);
                                            ScreenshotsHistoryActions.enableHistoryUpdate();
                                            ScreenshotsPageActions.performSearch();
                                }} />
                        </AltContainer>

                    </div>

                    <AltContainer store={ScreenshotsPageStore}>
                        <ScreenshotsGrid
                            onScreenshotsTextUnitTargetClick={(e, textUnit, locale) => this.onScreenshotsTextUnitTargetClick(e, textUnit, locale)}
                            onScreenshotsTextUnitNameClick={(e, textUnit) => this.onScreenshotsTextUnitNameClick(e, textUnit)}
                            onScreenshotClicked={this.onScreenshotClicked}
                            onLocaleClick={ (locale) => {
                                                ScreenshotsHistoryActions.disableHistoryUpdate();
                                                ScreenshotsPaginatorActions.changeCurrentPageNumber(1);
                                                ScreenshotsSearchTextActions.changeSearchText("");
                                                ScreenshotsLocaleActions.changeSelectedBcp47Tags(locale);
                                                ScreenshotsHistoryActions.enableHistoryUpdate();
                                                ScreenshotsPageActions.performSearch();
                            }}
                            onNameClick={ (name) => {
                                                    ScreenshotsHistoryActions.disableHistoryUpdate();
                                                    ScreenshotsSearchTextActions.changeSearchText(name);
                                                    ScreenshotsLocaleActions.changeSelectedBcp47Tags(
                                                            ScreenshotsRepositoryStore.getAllBcp47TagsForRepositoryIds(
                                                                    ScreenshotsRepositoryStore.getState().selectedRepositoryIds));
                                                    ScreenshotsSearchTextActions.changeSearchType(SearchParamsStore.SEARCH_TYPES.EXACT);
                                                    ScreenshotsSearchTextActions.changeSearchAttribute(ScreenshotsSearchTextStore.SEARCH_ATTRIBUTES_SCREENSHOT);
                                                    ScreenshotsPaginatorActions.changeCurrentPageNumber(1);
                                                    ScreenshotsHistoryActions.enableHistoryUpdate();
                                                    ScreenshotsPageActions.performSearch();
                            }}
                            onStatusGlyphClick={(screenshotIdx) => {
                                                        ScreenshotsReviewModalActions.openWithScreenshot(screenshotIdx);
                            }}
                            onStatusChanged={ScreenshotActions.changeStatus}
                            />
                    </AltContainer>

                    <AltContainer store={ScreenshotsReviewModalStore}>
                        <ScreenshotReviewModal
                            onCancel={ScreenshotsReviewModalActions.close}
                            onSave={ScreenshotsReviewModalActions.save}
                            onCommentChanged={ScreenshotsReviewModalActions.changeComment}
                            onStatusChanged={ScreenshotsReviewModalActions.changeStatus}
                            />
                    </AltContainer>
                </div>
        );
    }
}

export default withRouter(ScreenshotsPage);
