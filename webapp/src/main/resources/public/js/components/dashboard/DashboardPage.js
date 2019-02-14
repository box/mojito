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
import DashboardHistoryActions from "../../actions/dashboard/DashboardHistoryActions";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import RepositoryStore from "../../stores/RepositoryStore";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchConstants from "../../utils/SearchConstants";

class DashboardPage extends React.Component {

    render() {

        return (
            <div>
                <AltContainer stores={
                    {
                        disabled: function (props) {
                            return {
                                store: DashboardStore,
                                value: DashboardStore.getState().selectedBranchTextUnitIds.length === 0
                            };
                        }
                    }}>
                    <DashboardAddScreenshotButton
                        onClick={() => {
                            DashboardScreenshotUploadActions.openWithBranch();
                        }}
                    />
                </AltContainer>

                <AltContainer store={DashboardSearchParamStore}>
                    <DashboardSearchText
                        onDashboardSearchTextChanged={
                            (text) => {
                                DashboardHistoryActions.disableHistoryUpdate();
                                DashboardSearchParamsActions.changeSearchText(text);
                            }
                        }

                        onPerformSearch={() => {
                            DashboardHistoryActions.disableHistoryUpdate();
                            DashboardPaginatorActions.changeCurrentPageNumber(1);
                            DashboardSearchParamsActions.changeOnlyMyBranches(false);
                            DashboardSearchParamsActions.changeDeleted(true);
                            DashboardSearchParamsActions.changeUndeleted(true);
                            DashboardHistoryActions.enableHistoryUpdate();
                            DashboardPageActions.getBranches();
                        }}
                    />
                </AltContainer>

                <AltContainer store={DashboardSearchParamStore}>
                    <DashboardStatusDropdown
                        onDeletedChanged={(deleted) => {
                            DashboardHistoryActions.disableHistoryUpdate();
                            DashboardSearchParamsActions.changeDeleted(deleted);
                            DashboardPageActions.changeSelectedBranchTextUnitIds([]);
                            DashboardHistoryActions.enableHistoryUpdate();
                            DashboardPageActions.getBranches();
                        }}

                        onUndeletedChanged={(undeleted) => {
                            DashboardHistoryActions.disableHistoryUpdate();
                            DashboardSearchParamsActions.changeUndeleted(undeleted);
                            DashboardPageActions.changeSelectedBranchTextUnitIds([]);
                            DashboardHistoryActions.enableHistoryUpdate();
                            DashboardPageActions.getBranches();
                        }}

                        onOnlyMyBranchesChanged={(onlyMyBranches) => {
                            DashboardHistoryActions.disableHistoryUpdate();
                            DashboardSearchParamsActions.changeOnlyMyBranches(onlyMyBranches);
                            DashboardPageActions.changeSelectedBranchTextUnitIds([]);
                            DashboardHistoryActions.enableHistoryUpdate();
                            DashboardPageActions.getBranches();
                        }}
                    />
                </AltContainer>

                <AltContainer store={DashboardPaginatorStore}>
                    <Paginator
                        onPreviousPageClicked={() => {
                            DashboardHistoryActions.disableHistoryUpdate();
                            DashboardPaginatorActions.goToPreviousPage();
                            DashboardPageActions.changeSelectedBranchTextUnitIds([]);
                            DashboardHistoryActions.enableHistoryUpdate();
                            DashboardPageActions.getBranches();
                        }}
                        onNextPageClicked={() => {
                            DashboardHistoryActions.disableHistoryUpdate();
                            DashboardPaginatorActions.goToNextPage();
                            DashboardPageActions.changeSelectedBranchTextUnitIds([]);
                            DashboardHistoryActions.enableHistoryUpdate();
                            DashboardPageActions.getBranches();
                        }}/>
                </AltContainer>

                <div className="clear"/>

                <AltContainer store={DashboardStore}>
                    <DashboardSearchResults
                        onChangeSelectedBranchTextUnits={(selectedBranchTextUnitIds) => {
                            DashboardPageActions.changeSelectedBranchTextUnitIds(selectedBranchTextUnitIds);
                        }}

                        onChangeOpenBranchStatistic={(branchStatisticId) => {
                            DashboardPageActions.changeOpenBranchStatistic(branchStatisticId);
                            DashboardPageActions.changeSelectedBranchTextUnitIds([]);
                        }}

                        onShowBranchScreenshotsClick={(branchStatisticId) => {
                            DashboardScreenshotViewerActions.open(branchStatisticId);
                        }}

                        onNeedTranslationClick={(branchStatistic, tmTextUnitId, forTranslation) => {

                            let repoIds = [branchStatistic.branch.repository.id];

                            let params = {
                                "changedParam": SearchConstants.UPDATE_ALL,
                                "repoIds": repoIds,
                                "branchId": branchStatistic.branch.id,
                                "bcp47Tags": RepositoryStore.getAllBcp47TagsForRepositoryIds(repoIds),
                                "status": forTranslation ? SearchParamsStore.STATUS.FOR_TRANSLATION: SearchParamsStore.STATUS.ALL,
                            }

                            if (tmTextUnitId != null) {
                                params["tmTextUnitIds"] = [tmTextUnitId];
                            }

                            WorkbenchActions.searchParamsChanged(params);
                            this.props.router.push("/workbench", null, null);
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
