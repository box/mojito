import _ from "lodash";
import React from "react";
import createReactClass from 'create-react-class';
import {withRouter} from "react-router";
import FluxyMixin from "alt-mixins/FluxyMixin";
import LocalesDropdown from "./LocalesDropdown";
import RepositoryDropDown from "./RepositoryDropdown";
import SearchResults from "./SearchResults";
import StatusDropdown from "./StatusDropdown";
import SearchText from "./SearchText";
import AltContainer from "alt-container";
import GitBlameStore from "../../stores/workbench/GitBlameStore";
import GitBlameInfoModal from "./GitBlameInfoModal";
import GitBlameActions from "../../actions/workbench/GitBlameActions";
import ScreenshotViewerModal from "../screenshots/ScreenshotViewerModal";
import GitBlameScreenshotViewerActions from "../../actions/workbench/GitBlameScreenshotViewerActions";
import GitBlameScreenshotViewerStore from "../../stores/workbench/GitBlameScreenshotViewerStore";
import UrlHelper from "../../utils/UrlHelper";
import TranslationHistoryStore from "../../stores/workbench/TranslationHistoryStore";
import TranslationHistoryModal from "./TranslationHistoryModal";
import TranslationHistoryActions from "../../actions/workbench/TranslationHistoryActions";
import ShareSearchParamsModalStore from "../../stores/workbench/ShareSearchParamsModalStore";
import ShareSearchParamsModal from "./ShareSearchParamsModal";
import ShareSearchParamsModalActions from "../../actions/workbench/ShareSearchParamsModalActions";
import ShareSearchParamsButton from "./ShareSearchParamsButton";

let Workbench = createReactClass({
    displayName: 'Workbench',
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onGitBlameStoreUpdated": GitBlameStore,
            "onTranslationHistoryStoreUpdated": TranslationHistoryStore
        }
    },

    onGitBlameStoreUpdated(store) {
        this.setState({"isShowGitBlameModal": store.show});
    },

    onTranslationHistoryStoreUpdated(store) {
        this.setState({"isShowTranslationHistoryModal": store.show});
    },

    /**
     * Create query string given SearchParams
     *
     * @param searchParams
     * @return {*}
     */
    buildQuery: function (searchParams) {
        let cloneParam = _.clone(searchParams);
        delete cloneParam["changedParam"];
        return UrlHelper.toQueryString(cloneParam);
    },

    render: function () {
        return (
            <div>
                <div className="pull-left">
                    <RepositoryDropDown />
                    <LocalesDropdown />
                </div>

                <SearchText />
                <StatusDropdown/>
                <ShareSearchParamsButton onClick={ShareSearchParamsModalActions.open}/>

                <div className="mtl mbl">
                    <SearchResults />
                </div>
                <AltContainer store={GitBlameStore}>
                    <GitBlameInfoModal
                        onCloseModal={GitBlameActions.close}
                        onViewScreenshotClick={(branchScreenshots) => {
                            GitBlameScreenshotViewerActions.openScreenshotsViewer(branchScreenshots);
                        }}/>
                </AltContainer>
                <AltContainer store={GitBlameScreenshotViewerStore}>
                    <ScreenshotViewerModal
                        onGoToPrevious={() => {
                            GitBlameScreenshotViewerActions.goToPrevious();
                        }}
                        onGoToNext={() => {
                            GitBlameScreenshotViewerActions.goToNext();
                        }}
                        onClose={() => {
                            GitBlameScreenshotViewerActions.closeScreenshotsViewer();
                        }}
                        onDelete={() => {
                            GitBlameScreenshotViewerActions.delete();
                        }}
                    />
                </AltContainer>
                <AltContainer store={TranslationHistoryStore}>
                    <TranslationHistoryModal onCloseModal={TranslationHistoryActions.close}
                                             onChangeOpenTmTextUnitVariant={TranslationHistoryActions.changeOpenTmTextUnitVariant}/>
                </AltContainer>

                <AltContainer store={ShareSearchParamsModalStore}>
                    <ShareSearchParamsModal
                        onCancel={ShareSearchParamsModalActions.close}
                        onCopy={(url) => {
                            navigator.clipboard.writeText(url)
                                .then(ShareSearchParamsModalActions.close)
                                .catch(err => ShareSearchParamsModalActions.setError(ShareSearchParamsModalStore.ERROR_TYPES.COPY_TO_CLIPBOARD));
                        }}/>
                </AltContainer>
            </div>
        );
    },
});

export default withRouter(Workbench);
