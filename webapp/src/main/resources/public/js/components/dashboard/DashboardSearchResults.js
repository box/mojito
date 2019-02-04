import React from "react";
import PropTypes from 'prop-types';
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, ButtonToolbar, TextUnitSelector, Row, Col} from "react-bootstrap";
import BranchStatistic from "./BranchStatistic";
import ScreenshotUploadModal from "./ScreenshotUploadModal";
import DashboardStore from "../../stores/dashboard/DashboardStore";
import DashboardPageActions from "../../actions/dashboard/DashboardPageActions";


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
                <div className="mtl mbl">
                    <div className="col-xs-6">
                        <ButtonToolbar>
                            <Button bsStyle="primary" bsSize="small" disabled={actionButtonsDisabled}
                                    onClick={this.props.openScreenshotUploadModal}>
                                <FormattedMessage id="dashboard.actions.addScreenshot"/>
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


                        {/*TODO(ja) reuse pagingator*/}
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
                    <div className="textunit-toolbar-clear" />
                </div>


                <Row>
                    <Col md={4}>
                         <FormattedMessage id="repositories.table.header.name"/>
                    </Col>
                    <Col md={4}>
                        <FormattedMessage id="repositories.table.header.needsTranslation"/>
                    </Col>
                    <Col md={4}>
                        <FormattedMessage id="repositories.table.header.screenshot"/>
                    </Col>
                </Row>

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