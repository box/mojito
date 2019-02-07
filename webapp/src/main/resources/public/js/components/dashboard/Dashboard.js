import React from "react";
import {withRouter} from "react-router";
import AltContainer from "alt-container";

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

class Dashboard extends React.Component {

    render() {

        return (
            <div>
                <AltContainer store={DashboardSearchParamStore}>
                    <DashboardSearchText
                        onDashboardSearchTextChanged={
                            (text) => {
                                DashboardSearchParamsActions.changeSearchText(text);
                            }
                        }
                        onPerformSearch={() => {
                            // TODO reset pagination
                            // DashboardPaginatorAction.changeCurrentPageNumber(1);
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

                <AltContainer store={DashboardStore}>
                    <DashboardSearchResults
                        onTextUnitCheckboxClick={(indexTuple) => {
                            DashboardPageActions.textUnitCheckboxChanged(indexTuple);
                        }}
                        onBranchCollapseClick={(index) => {
                            DashboardPageActions.onBranchCollapseChange(index);
                        }}

                        onAddScreenshotClick={() => {
                            DashboardScreenshotUploadActions.openWithBranch();
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

export default withRouter(Dashboard);
