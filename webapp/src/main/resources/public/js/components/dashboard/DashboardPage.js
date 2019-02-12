import React from "react";
import {withRouter} from "react-router";
import AltContainer from "alt-container";

import DashboardAddScreenshotButton from "../../components/dashboard/DashboardAddScreenshotButton";
import DashboardStore from "../../stores/dashboard/DashboardStore";
import DashboardSearchText from "./DashboardSearchText";
import DashboardStatusDropdown from "./DashboardStatusDropdown";
import DashboardSearchParamStore from "../../stores/dashboard/DashboardSearchParamStore";
import DashboardPageActions from "../../actions/dashboard/DashboardPageActions";
import DashboardSearchParamsActions from "../../actions/dashboard/DashboardSearchParamsActions";
import DashboardSearchResults from "./DashboardSearchResults";
import DashboardScreenshotUploadModal from "./DashboardScreenshotUploadModal";
import DashboardScreenshotUploadModalStore from "../../stores/dashboard/DashboardScreenshotUploadModalStore";
import DashboardScreenshotUploadActions from "../../actions/dashboard/DashboardScreenshotUploadActions";
import DashboardScreenshotViewerModal from "./DashboardScreenshotViewerModal";
import DashboardScreenshotViewerStore from "../../stores/dashboard/DashboardScreenshotViewerStore";
import DashboardScreenshotViewerActions from "../../actions/dashboard/DashboardScreenshotViewerActions";
import Paginator from "../../components/screenshots/Paginator";
import DashboardPaginatorStore from "../../stores/dashboard/DashboardPaginatorStore";
import DashboardPaginatorActions from "../../actions/dashboard/DashboardPaginatorActions";

class DashboardPage extends React.Component {

    render() {

        return (
            <div>
                <AltContainer stores={
                    {
                        disabled: function (props) {
                            return {
                                store: DashboardStore,
                                value: DashboardStore.getState().numberOfTextUnitChecked === 0
                            };
                        }
                    }}>
                    <DashboardAddScreenshotButton
                        onClick={() => {
                            DashboardScreenshotUploadActions.openWithBranch();
                        }}
                        disabled={false}
                    />
                </AltContainer>

                <AltContainer store={DashboardSearchParamStore}>
                    <DashboardSearchText
                        onDashboardSearchTextChanged={
                            (text) => {
                                DashboardSearchParamsActions.changeSearchText(text);
                            }
                        }
                        onPerformSearch={() => {
                            DashboardPaginatorActions.changeCurrentPageNumber(1);
                            DashboardPageActions.getBranches();
                        }}
                    />
                </AltContainer>

                <AltContainer store={DashboardSearchParamStore}>
                    <DashboardStatusDropdown
                        onFilterSelected={(filter) => {
                            DashboardSearchParamsActions.changeSearchFilter(filter);
                        }}
                    />
                </AltContainer>

                <AltContainer store={DashboardPaginatorStore}>
                    <Paginator
                        onPreviousPageClicked={() => {
                            //TODO(ja) implement history
                            // ScreenshotsHistoryActions.disableHistoryUpdate();
                            DashboardPaginatorActions.goToPreviousPage();
                            DashboardPageActions.resetAllSelectedTextUnitsInCurrentPage();
                            // ScreenshotsHistoryActions.enableHistoryUpdate();
                            DashboardPageActions.getBranches();
                        }}
                        onNextPageClicked={() => {
                            // ScreenshotsHistoryActions.disableHistoryUpdate();
                            DashboardPaginatorActions.goToNextPage();
                            DashboardPageActions.resetAllSelectedTextUnitsInCurrentPage();
                            // ScreenshotsHistoryActions.enableHistoryUpdate();
                            DashboardPageActions.getBranches();
                        }}/>
                </AltContainer>

                <div className="clear"/>

                <AltContainer store={DashboardStore}>
                    <DashboardSearchResults
                        onTextUnitCheckboxClick={(indexTuple) => {
                            DashboardPageActions.textUnitCheckboxChanged(indexTuple);
                        }}
                        onBranchCollapseClick={(index) => {
                            DashboardPageActions.onBranchCollapseChange(index);
                        }}

                        onShowBranchScreenshotsClick={(index) => {
                            DashboardScreenshotViewerActions.open(index);
                        }}
                    />
                </AltContainer>

                <AltContainer store={DashboardScreenshotUploadModalStore}>
                    <DashboardScreenshotUploadModal
                        onCancel={() => {
                            DashboardScreenshotUploadActions.close();
                        }}

                        onSelectedFileChange={(files) => {
                            DashboardScreenshotUploadActions.changeSelectedFiles(files);
                        }}

                        onUpload={() => {
                            DashboardScreenshotUploadActions.uploadScreenshotImage();
                        }}
                    />
                </AltContainer>

                <AltContainer store={DashboardScreenshotViewerStore}>
                    <DashboardScreenshotViewerModal
                        onGoToPrevious={() => {
                            DashboardScreenshotViewerActions.goToPrevious();
                        }}
                        onGoToNext={() => {
                            DashboardScreenshotViewerActions.goToNext();
                        }}
                        onClose={() => {
                            DashboardScreenshotViewerActions.close();
                        }}
                    />
                </AltContainer>

            </div>
        );
    }
};

export default withRouter(DashboardPage);
