import React from "react";
import PropTypes from 'prop-types';
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, ButtonToolbar, Table} from "react-bootstrap";
import RepositoryHeaderColumn from "../repositories/RepositoryHeaderColumn";
import BranchStatistic from "./BranchStatistic";
import ScreenshotUploadModal from "./ScreenshotUploadModal";
import TextUnitSelector from "./TextUnitSelector";
import DashboardPageActions from "../../actions/dashboard/DashboardPageActions";
import DashboardStore from "../../stores/dashboard/DashboardStore";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchConstants from "../../utils/SearchConstants";

class DashboardSearchResults extends React.Component {

    static propTypes = {
        "uploadScreenshotStatus": PropTypes.string.isRequired,
        "currentPageNumber": PropTypes.number.isRequired,
        "showScreenshotUploadModal": PropTypes.bool.isRequired,
        "branchStatistics": PropTypes.array.isRequired,
        "isBranchOpen": PropTypes.array.isRequired,
        "textUnitChecked": PropTypes.array.isRequired,
        "onTextUnitCheckboxClick": PropTypes.func.isRequired,
        "onBranchCollapseClick": PropTypes.func.isRequired,
        "onChooseImageClick": PropTypes.func.isRequired,
        "onUploadImageClick": PropTypes.func.isRequired,
        "openScreenshotUploadModal": PropTypes.func.isRequired,
        "closeScreenshotUploadModal": PropTypes.func.isRequired,
    };

    createBranchStatisticComponent(branchStatistic, arrayIndex) {
        return (
            <BranchStatistic
                key={branchStatistic.id}
                branchStatistic={branchStatistic}
                isBranchOpen={this.props.isBranchOpen[arrayIndex]}
                textUnitChecked={this.props.textUnitChecked[arrayIndex]}
                onUploadImageClick={
                    () => {
                        this.props.onUploadImageClick(arrayIndex)
                    }
                }
                onTextUnitForScreenshotUploadClick={
                    (index) => {
                        this.props.onTextUnitCheckboxClick({index0: arrayIndex, index1: index})
                    }
                }
                onBranchCollapseClick={
                    () => {
                        this.props.onBranchCollapseClick(arrayIndex)
                    }
                }
            />
        )

    }

    render() {
        let actionButtonsDisabled = this.props.textUnitChecked.every(function (row) {
            return row.every(function (e) {
                return !e;
            })

        });
        let previousPageButtonDisabled = DashboardStore.getState().hasNext;
        let nextPageButtonDisabled = DashboardStore.getState().hasPrevious;

        return (
            <div>
                <div>
                    <div className="pull-left">
                        <ButtonToolbar>
                            <Button bsSize="small" disabled={actionButtonsDisabled}
                                    onClick={this.props.openScreenshotUploadModal}>
                                <FormattedMessage id="label.upload"/>
                            </Button>
                        </ButtonToolbar>
                    </div>
                    <div className="pull-right">
                        <TextUnitSelector
                            selectAllTextUnitsInCurrentPage={() => {
                                DashboardPageActions.selectAllTextUnitsInCurrentPage();
                            }}
                            resetAllSelectedTextUnitsInCurrentPage={() => {
                                DashboardPageActions.resetAllSelectedTextUnitsInCurrentPage();
                            }}
                        />
                        <Button bsSize="small" disabled={previousPageButtonDisabled}
                                onClick={() => {DashboardPageActions.fetchPreviousPage()}}><span
                            className="glyphicon glyphicon-chevron-left"></span></Button>
                        <label className="mls mrs default-label current-pageNumber">
                            {this.props.currentPageNumber + 1}
                        </label>
                        <Button bsSize="small" disabled={nextPageButtonDisabled}
                                onClick={() => {DashboardPageActions.fetchNextPage()}}><span
                            className="glyphicon glyphicon-chevron-right"></span></Button>
                    </div>
                </div>

                <div className="plx prx">
                    <Table className="repo-table table-padded-sides">
                        <thead>
                        <tr>
                            <RepositoryHeaderColumn className="col-md-3"
                                                    columnNameMessageId="repositories.table.header.name"/>
                            <RepositoryHeaderColumn className="col-md-3"
                                                    columnNameMessageId="repositories.table.header.needsTranslation"/>
                            <RepositoryHeaderColumn className="col-md-3"
                                                    columnNameMessageId="dashboard.table.header.screenshot"/>
                        </tr>
                        </thead>
                    </Table>
                </div>
                {this.props.branchStatistics.map(this.createBranchStatisticComponent.bind(this))}
                <ScreenshotUploadModal
                    uploadScreenshotStatus={this.props.uploadScreenshotStatus}
                    showModal={this.props.showScreenshotUploadModal}
                    closeModal={this.props.closeScreenshotUploadModal}
                    onUploadImageClick={this.props.onUploadImageClick}
                    onChooseImageClick={this.props.onChooseImageClick}
                />
            </div>

        );
    }

}

export default injectIntl(DashboardSearchResults);