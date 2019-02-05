import React from "react";
import PropTypes from 'prop-types';
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, ButtonToolbar, Col, Collapse, Glyphicon, Grid, Row, TextUnitSelector} from "react-bootstrap";
import BranchStatistic from "./BranchStatistic";
import ScreenshotUploadModal from "./ScreenshotUploadModal";
import DashboardStore from "../../stores/dashboard/DashboardStore";


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

    branch(branchStatistic, arrayIndex) {
        return (
            <Grid fluid={true}>
                <Row className="bms" className="dashboard-branchstatistic-branch">
                    <Col md={4}>
                        <Button bsSize="xsmall" onClick={() => this.props.onBranchCollapseClick(arrayIndex)}>
                            <Glyphicon glyph={this.props.isBranchOpen[arrayIndex] ? "chevron-down" : "chevron-right"}
                                       className="color-gray-light"/>
                        </Button>
                        <span>{branchStatistic.branch.name}</span>
                    </Col>
                    <Col md={4}>
                        ??
                    </Col>
                    <Col md={4}>
                        x
                    </Col>
                </Row>
            </Grid>
        );
    }

    col(branchStatistic, arrayIndex) {
        return (
            <div>
                <Collapse in={this.props.isBranchOpen[arrayIndex]}>
                    <Grid fluid={true}>
                        <Row className="bms" className="dashboard-branchstatistic-branch-textunit">
                            <Col md={4}>
                                {"tu+" + arrayIndex}
                            </Col>
                            <Col md={4}>
                                ??
                            </Col>
                            <Col md={4}>
                                x
                            </Col>
                        </Row>
                    </Grid>
                </Collapse>
            </div>
        );
    }

    render() {
        let actionButtonsDisabled = this.props.textUnitChecked.every(function (row) {
            return row.every(function (e) {
                return !e;
            })

        });

        // function / reuse pagination
        let previousPageButtonDisabled = DashboardStore.getState().hasNext;
        let nextPageButtonDisabled = DashboardStore.getState().hasPrevious;

        return (
            <div>

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

                        {/*<div className="pull-right">*/}
                        {/*<TextUnitSelector*/}
                        {/*selectAllTextUnitsInCurrentPage={() => {*/}
                        {/*DashboardPageActions.selectAllTextUnitsInCurrentPage();*/}
                        {/*}}*/}
                        {/*resetAllSelectedTextUnitsInCurrentPage={() => {*/}
                        {/*DashboardPageActions.resetAllSelectedTextUnitsInCurrentPage();*/}
                        {/*}}*/}
                        {/*/>*/}
                        {/**/}
                        {/*<Button bsSize="small" disabled={previousPageButtonDisabled}*/}
                        {/*onClick={() => {*/}
                        {/*DashboardPageActions.fetchPreviousPage()*/}
                        {/*}}><span*/}
                        {/*className="glyphicon glyphicon-chevron-left"></span></Button>*/}
                        {/*<label className="mls mrs default-label current-pageNumber">*/}
                        {/*{this.props.currentPageNumber + 1}*/}
                        {/*</label>*/}

                        {/*<Button bsSize="small" disabled={nextPageButtonDisabled}*/}
                        {/*onClick={() => {*/}
                        {/*DashboardPageActions.fetchNextPage()*/}
                        {/*}}><span*/}
                        {/*className="glyphicon glyphicon-chevron-right"></span></Button>*/}
                        {/*</div>*/}
                        <div className="clear"/>
                    </div>

                    <Grid fluid={true}>
                        <Row className="bms" className="dashboard-branchstatistic-header">
                            <Col md={4}>
                                <FormattedMessage id="dashboard.table.header.branch"/>
                            </Col>
                            <Col md={4}>
                                <FormattedMessage id="repositories.table.header.needsTranslation"/>
                            </Col>
                            <Col md={4}>
                                <FormattedMessage id="dashboard.table.header.screenshot"/>
                            </Col>
                        </Row>
                    </Grid>


                    {this.props.branchStatistics.map(this.branch.bind(this))}
                    {this.props.branchStatistics.map(this.col.bind(this))}

                </div>

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