import BranchStatisticClient from "../../sdk/BranchStatisticClient";
import BranchesPageActions from "./BranchesPageActions";
import BranchStatisticSearcherParameters from "../../sdk/BranchStatisticSearcherParameters";
import BranchesSearchParamStore from "../../stores/branches/BranchesSearchParamStore";
import BranchesPaginatorStore from "../../stores/branches/BranchesPaginatorStore";
import UserHelper from "../../utils/UserHelper";

const BranchesDataSource = {
    performBranchesSearch: {
        remote() {
            let returnEmpty = false;

            let branchesSearchParamState = BranchesSearchParamStore.getState();
            let branchesPaginatorState = BranchesPaginatorStore.getState();
            let branchStatisticSearcherParameters = new BranchStatisticSearcherParameters();

            if (branchesSearchParamState.searchText) {
                branchStatisticSearcherParameters.search(branchesSearchParamState.searchText);
            }

            if (branchesSearchParamState.onlyMyBranches) {
                branchStatisticSearcherParameters.createdByUserName(UserHelper.getUsername());
            }

            if (!branchesSearchParamState.deleted && !branchesSearchParamState.undeleted) {
                returnEmpty = true;
            } else if (branchesSearchParamState.deleted && !branchesSearchParamState.undeleted) {
                branchStatisticSearcherParameters.deleted(true);
            } else if (!branchesSearchParamState.deleted && branchesSearchParamState.undeleted) {
                branchStatisticSearcherParameters.deleted(false);
            }

            if (!branchesSearchParamState.empty && !branchesSearchParamState.notEmpty) {
                returnEmpty = true;
            } else if (branchesSearchParamState.empty && !branchesSearchParamState.notEmpty) {
                branchStatisticSearcherParameters.empty(true);
            } else if (!branchesSearchParamState.empty && branchesSearchParamState.notEmpty) {
                branchStatisticSearcherParameters.empty(false);
            }

            branchStatisticSearcherParameters.page(branchesPaginatorState.currentPageNumber - 1);
            branchStatisticSearcherParameters.size(branchesPaginatorState.limit);

            let promise;

            if (returnEmpty) {
                promise = new Promise(function (resolve, reject) {
                    resolve({
                        "hasNext": false,
                        "size": 10,
                        "content": [],
                        "hasPrevious": false,
                        "number": 0,
                        "first": true,
                        "numberOfElements": 0,
                        "totalPages": 1,
                        "totalElements": 0,
                        "last": true
                    });
                });
            } else {
                promise = BranchStatisticClient.getBranches(branchStatisticSearcherParameters);
            }

            return promise;
        },
        success: BranchesPageActions.getBranchesSuccess,
        error: BranchesPageActions.getBranchesError
    }
};

export default BranchesDataSource;