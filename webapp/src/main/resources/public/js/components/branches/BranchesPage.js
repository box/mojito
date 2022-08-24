import React from "react";
import {withRouter} from "react-router";
import AltContainer from "alt-container";

import BranchesAddScreenshotButton from ".//BranchesAddScreenshotButton";
import BranchesStore from "../../stores/branches/BranchesStore";
import BranchesSearchText from "./BranchesSearchText";
import BranchesStatusDropdown from "./BranchesStatusDropdown";
import BranchesSearchParamStore from "../../stores/branches/BranchesSearchParamStore";
import BranchesPageActions from "../../actions/branches/BranchesPageActions";
import BranchesSearchParamsActions from "../../actions/branches/BranchesSearchParamsActions";
import BranchesSearchResults from "./BranchesSearchResults";
import BranchesScreenshotUploadModal from "./BranchesScreenshotUploadModal";
import BranchesScreenshotUploadModalStore from "../../stores/branches/BranchesScreenshotUploadModalStore";
import BranchesScreenshotUploadActions from "../../actions/branches/BranchesScreenshotUploadActions";
import BranchesScreenshotViewerModal from "./BranchesScreenshotViewerModal";
import BranchesScreenshotViewerStore from "../../stores/branches/BranchesScreenshotViewerStore";
import BranchesScreenshotViewerActions from "../../actions/branches/BranchesScreenshotViewerActions";
import BranchesPaginatorStore from "../../stores/branches/BranchesPaginatorStore";
import BranchesPaginatorActions from "../../actions/branches/BranchesPaginatorActions";
import BranchesDataSource from "../../actions/branches/BranchesHistoryActions";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import Paginator from "../widgets/Paginator";
import RepositoryStore from "../../stores/RepositoryStore";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchConstants from "../../utils/SearchConstants";
import GitBlameScreenshotViewerActions from "../../actions/workbench/GitBlameScreenshotViewerActions";
import ScreenshotViewerActions from "../../actions/screenshots/ScreenshotViewerActions";


class BranchesPage extends React.Component {

    render() {

        return (
            <div>
                <AltContainer stores={
                    {
                        disabled: function (props) {
                            return {
                                store: BranchesStore,
                                value: BranchesStore.getState().selectedBranchTextUnitIds.length === 0
                            };
                        }
                    }}>
                    <BranchesAddScreenshotButton
                        onClick={() => {
                            BranchesScreenshotUploadActions.openWithBranch();
                        }}
                    />
                </AltContainer>

                <AltContainer store={BranchesSearchParamStore}>
                    <BranchesSearchText
                        onBranchesSearchTextChanged={
                            (text) => {
                                BranchesDataSource.disableHistoryUpdate();
                                BranchesSearchParamsActions.changeSearchText(text);
                            }
                        }

                        onPerformSearch={() => {
                            BranchesDataSource.disableHistoryUpdate();
                            BranchesPaginatorActions.changeCurrentPageNumber(1);
                            BranchesSearchParamsActions.changeOnlyMyBranches(false);
                            BranchesSearchParamsActions.changeDeleted(true);
                            BranchesSearchParamsActions.changeUndeleted(true);
                            BranchesSearchParamsActions.changeEmpty(true);
                            BranchesSearchParamsActions.changeNotEmpty(true);
                            BranchesDataSource.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}
                    />
                </AltContainer>

                <AltContainer store={BranchesSearchParamStore}>
                    <BranchesStatusDropdown
                        onDeletedChanged={(deleted) => {
                            BranchesDataSource.disableHistoryUpdate();
                            BranchesSearchParamsActions.changeDeleted(deleted);
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesDataSource.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}

                        onUndeletedChanged={(undeleted) => {
                            BranchesDataSource.disableHistoryUpdate();
                            BranchesSearchParamsActions.changeUndeleted(undeleted);
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesDataSource.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}

                        onEmptyChanged={(empty) => {
                            BranchesDataSource.disableHistoryUpdate();
                            BranchesSearchParamsActions.changeEmpty(empty);
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesDataSource.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}

                        onNotEmptyChanged={(notEmpty) => {
                            BranchesDataSource.disableHistoryUpdate();
                            BranchesSearchParamsActions.changeNotEmpty(notEmpty);
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesDataSource.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}

                        onOnlyMyBranchesChanged={(onlyMyBranches) => {
                            BranchesDataSource.disableHistoryUpdate();
                            BranchesSearchParamsActions.changeOnlyMyBranches(onlyMyBranches);
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesDataSource.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}

                        onCreatedBeforeChanged={(createdBefore) => {
                            BranchesDataSource.disableHistoryUpdate();
                            BranchesSearchParamsActions.changeCreatedBefore(createdBefore)
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesDataSource.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}

                        onCreatedAfterChanged={(createdAfter) => {
                            BranchesDataSource.disableHistoryUpdate();
                            BranchesSearchParamsActions.changeCreatedAfter(createdAfter)
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesDataSource.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}
                    />
                </AltContainer>

                <AltContainer store={BranchesPaginatorStore}>
                    <Paginator
                        onPreviousPageClicked={() => {
                            BranchesDataSource.disableHistoryUpdate();
                            BranchesPaginatorActions.goToPreviousPage();
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesDataSource.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}
                        onNextPageClicked={() => {
                            BranchesDataSource.disableHistoryUpdate();
                            BranchesPaginatorActions.goToNextPage();
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesDataSource.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}/>
                </AltContainer>

                <AltContainer store={BranchesStore}>
                    <BranchesSearchResults
                        onChangeSelectedBranchTextUnits={(selectedBranchTextUnitIds) => {
                            BranchesPageActions.changeSelectedBranchTextUnitIds(selectedBranchTextUnitIds);
                        }}

                        onChangeOpenBranchStatistic={(branchStatisticId) => {
                            BranchesPageActions.changeOpenBranchStatistic(branchStatisticId);
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                        }}

                        onShowBranchScreenshotsClick={(branchStatisticId) => {
                            BranchesScreenshotViewerActions.open(branchStatisticId);
                        }}

                        onNeedTranslationClick={(branchStatistic, tmTextUnitId, forTranslation) => {

                            let repoIds = [branchStatistic.branch.repository.id];

                            let params = {
                                "changedParam": SearchConstants.UPDATE_ALL,
                                "repoIds": repoIds,
                                "branchId": branchStatistic.branch.id,
                                "bcp47Tags": RepositoryStore.getAllBcp47TagsForRepositoryIds(repoIds, true),
                                "status": forTranslation ? SearchParamsStore.STATUS.FOR_TRANSLATION : SearchParamsStore.STATUS.ALL,
                            }

                            if (tmTextUnitId != null) {
                                params["tmTextUnitIds"] = [tmTextUnitId];
                            }

                            WorkbenchActions.searchParamsChanged(params);
                            this.props.router.push("/workbench", null, null);
                        }}

                        onTextUnitNameClick={(branchStatistic, tmTextUnitId) => {

                            let repoIds = [branchStatistic.branch.repository.id];

                            let params = {
                                "changedParam": SearchConstants.UPDATE_ALL,
                                "repoIds": repoIds,
                                "tmTextUnitIds": [tmTextUnitId],
                                "bcp47Tags": RepositoryStore.getAllBcp47TagsForRepositoryIds(repoIds, true),
                                "status": SearchParamsStore.STATUS.ALL,
                            }

                            WorkbenchActions.searchParamsChanged(params);
                            this.props.router.push("/workbench", null, null);
                        }}
                    />
                </AltContainer>

                <AltContainer store={BranchesScreenshotUploadModalStore}>
                    <BranchesScreenshotUploadModal
                        onCancel={() => {
                            BranchesScreenshotUploadActions.close();
                        }}

                        onSelectedFileChange={(files) => {
                            BranchesScreenshotUploadActions.changeSelectedFiles(files);
                        }}

                        onUpload={() => {
                            BranchesScreenshotUploadActions.uploadScreenshotImage();
                        }}
                    />
                </AltContainer>

                <AltContainer store={BranchesScreenshotViewerStore}>
                    <BranchesScreenshotViewerModal
                        onGoToPrevious={BranchesScreenshotViewerActions.goToPrevious}
                        onGoToNext={BranchesScreenshotViewerActions.goToNext}
                        onClose={BranchesScreenshotViewerActions.close}
                        onDelete={ScreenshotViewerActions.delete}
                    />
                </AltContainer>

            </div>
        );
    }
};

export default withRouter(BranchesPage);
