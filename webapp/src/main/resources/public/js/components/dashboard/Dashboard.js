import React from "react";
import {Link, withRouter} from "react-router";
import AltContainer from "alt-container";

import DashboardStore from "../../stores/dashboard/DashboardStore";
import DashboardSearchText from "./DashboardSearchText";
import DashboardStatusDropdown from "./DashboardStatusDropdown";
import DashboardSearchParamStore from "../../stores/dashboard/DashboardSearchParamStore";

import DashboardPageActions from "../../actions/dashboard/DashboardPageActions";
import DashboardSearchParamsActions from "../../actions/dashboard/DashboardSearchParamsActions";
import DashboardSearchResults from "./DashboardSearchResults";


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
                        onUploadImageClick={(index) => {
                            DashboardPageActions.uploadScreenshotImage(index);
                        }}
                        onChooseImageClick={(image) => {
                            DashboardPageActions.onImageChoose(image);
                        }}
                        onTextUnitCheckboxClick={(indexTuple) => {
                            DashboardPageActions.textUnitCheckboxChanged(indexTuple);
                        }}
                        onBranchCollapseClick={(index) => {
                            DashboardPageActions.onBranchCollapseChange(index);
                        }}
                        openScreenshotUploadModal={() => {
                            DashboardPageActions.onScreenshotUploadModalOpen();
                        }}
                        closeScreenshotUploadModal={() => {
                            DashboardPageActions.onScreenshotUploadModalClose();
                        }}
                    />
                </AltContainer>
            </div>
        );
    }
};

export default withRouter(Dashboard);
