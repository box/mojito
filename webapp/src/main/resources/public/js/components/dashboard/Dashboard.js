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

                        onShowBranchScreenshotClick={() => {
                            console.log("onShowBranchScreenshotClick");
                            //open modal
                            //get select branch, get selected
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


                <DashboardScreenshotViewerModal show={false} uploadDisabled={false} number={1} total={10}
                                                onGoToPrevious={() => { console.log("finish go to previous")}}
                                                onGoToNext={() => { console.log("finish go to next")}}
                                                onClose={() => {console.log("finish on close")}}

                />


            </div>
        );
    }
};

export default withRouter(Dashboard);
