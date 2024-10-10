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
import TranslationHistoryStore from "../../stores/workbench/TranslationHistoryStore";
import TranslationHistoryModal from "./TranslationHistoryModal";
import TranslationHistoryActions from "../../actions/workbench/TranslationHistoryActions";
import ShareSearchParamsModalStore from "../../stores/workbench/ShareSearchParamsModalStore";
import ShareSearchParamsModal from "./ShareSearchParamsModal";
import ShareSearchParamsModalActions from "../../actions/workbench/ShareSearchParamsModalActions";
import ShareSearchParamsButton from "./ShareSearchParamsButton";
import AuthorityService from "../../utils/AuthorityService";

let Workbench = createReactClass({
    displayName: 'Workbench',
    componentWillUnmount() {
        GitBlameActions.close();
        GitBlameScreenshotViewerActions.closeScreenshotsViewer();
        TranslationHistoryActions.close();
        ShareSearchParamsModalActions.close();
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
                        disableDelete={!AuthorityService.canEditScreenshots()}
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
