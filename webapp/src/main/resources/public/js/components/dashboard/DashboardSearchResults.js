import React from "react";
import PropTypes from 'prop-types';
import {FormattedDate, FormattedMessage, FormattedNumber, injectIntl} from "react-intl";
import {Button, ButtonToolbar, Col, Collapse, Glyphicon, Grid, OverlayTrigger, Row, Tooltip, Label} from "react-bootstrap";
import {Link} from "react-router";
import DashboardStore from "../../stores/dashboard/DashboardStore";
import RepositoryStore from "../../stores/RepositoryStore";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchConstants from "../../utils/SearchConstants";
import ClassNames from "classnames";


class DashboardSearchResults extends React.Component {

    static propTypes = {
        "currentPageNumber": PropTypes.number.isRequired,
        "branchStatistics": PropTypes.array.isRequired,
        "isBranchOpen": PropTypes.array.isRequired,
        "textUnitChecked": PropTypes.array.isRequired,
        "onTextUnitCheckboxClick": PropTypes.func.isRequired,
        "onBranchCollapseClick": PropTypes.func.isRequired,
        "onAddScreenshotClick": PropTypes.func.isRequired
    };

    /**
     * Update the Workbench search params to load the translation view for the selected repo
     *
     * @param {number} repoId
     */
    updateSearchParamsForNeedsTranslation(branchStatistic, textUnitId) {
        // TODO(ja) move to page ...
        let repoIds = [branchStatistic.branch.repository.id];

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": repoIds,
            "branchId": branchStatistic.branch.id,
            "tmTextUnitIds": textUnitId,
            "bcp47Tags": RepositoryStore.getAllBcp47TagsForRepositoryIds(repoIds),
        });
    }


    branch(branchStatistic, arrayIndex) {
        let rows = [];

        var className = ClassNames("dashboard-branchstatistic-branch", {"dashboard-branchstatistic-branch-open": false && this.props.isBranchOpen[arrayIndex]});

        rows.push((
            <Row key={"branchStatistic-" + arrayIndex} className={className}>
                <Col md={4} className="dashboard-branchstatistic-branch-col1">
                    <Button bsSize="xsmall" onClick={() => this.props.onBranchCollapseClick(arrayIndex)}>
                        <Glyphicon glyph={this.props.isBranchOpen[arrayIndex] ? "chevron-down" : "chevron-right"}
                                   className="color-gray-light"/>
                    </Button>
                    <span className="mlm">{branchStatistic.branch.name}</span>

                </Col>
                <Col md={2}>


                    {branchStatistic.forTranslationCount === 0 ?
                        <Label bsStyle="success" className="mrs clickable">
                            <FormattedMessage id="repositories.table.row.done"/>
                        </Label>
                        :
                        <Link
                            onClick={this.updateSearchParamsForNeedsTranslation.bind(this, branchStatistic)}
                            to='/workbench'>
                                <span className="dashboard-branchstatistic-needstranslation-counts"><FormattedNumber
                                    value={branchStatistic.forTranslationCount}/>&nbsp;</span>
                            (&nbsp;<FormattedMessage
                            values={{numberOfWords: branchStatistic.totalCount}}
                            id="repositories.table.row.numberOfWords"/>&nbsp;)
                        </Link>
                    }
                </Col>
                <Col md={1}>
                    -
                </Col>
                <Col md={2}>
                    <span>{branchStatistic.branch.createdByUser ? branchStatistic.branch.createdByUser.username : "-"}</span>
                </Col>
                <Col md={2}>
                    <span><FormattedDate value={branchStatistic.branch.createdDate} day="numeric" month="long"
                                         year="numeric"/></span>
                </Col>
                <Col md={1}>
                    <span>{branchStatistic.branch.deleted ? "Yes" : "No"}</span>
                </Col>
            </Row>
        ))
        ;

        className = ClassNames("dashboard-branchstatistic-branch-textunit", {"dashboard-branchstatistic-branch-open": this.props.isBranchOpen[arrayIndex]});

        branchStatistic.branchTextUnitStatistics.map((branchTextUnitStatistic, arrayIndexTextUnit) => {
            rows.push((
                <Collapse in={this.props.isBranchOpen[arrayIndex]}>
                    <div>
                        <Row key={"branchStatisticTextUnit-" + arrayIndex} className={className}>

                            <Col md={4} className="dashboard-branchstatistic-branch-col1">
                                <div>
                                    <div className="dashboard-branchstatistic-branch-col1-check"><input
                                        type="checkbox"
                                        checked={this.props.textUnitChecked[arrayIndex][arrayIndexTextUnit]}
                                        onChange={(index) => this.props.onTextUnitCheckboxClick({
                                            index0: arrayIndex,
                                            index1: arrayIndexTextUnit
                                        })}/>
                                    </div>
                                    <div className="plm">{branchTextUnitStatistic.tmTextUnit.name}</div>
                                    <div
                                        className="dashboard-branchstatistic-branch-col1-content">{branchTextUnitStatistic.tmTextUnit.content}</div>
                                </div>
                            </Col>

                            <Col md={2}>
                                <div>
                                    <Link
                                        onClick={this.updateSearchParamsForNeedsTranslation.bind(this, branchStatistic, branchTextUnitStatistic.tmTextUnit.id)}
                                        to='/workbench'>
                                    <span className="dashboard-branchstatistic-needstranslation-counts"><FormattedNumber
                                        value={branchTextUnitStatistic.forTranslationCount}/>&nbsp;</span>
                                        (&nbsp;<FormattedMessage
                                        values={{numberOfWords: branchTextUnitStatistic.totalCount}}
                                        id="repositories.table.row.numberOfWords"/>&nbsp;)
                                    </Link>
                                </div>
                            </Col>
                            <Col md={1}>
                                <div>
                                    {branchTextUnitStatistic.tmTextUnit.screenshotUploaded ?
                                        <Glyphicon glyph="ok"/> :
                                        <Glyphicon glyph="remove"/>}
                                </div>
                            </Col>
                        </Row>
                    </div>
                </Collapse>
            ));
        });

        rows.push((
            <Collapse in={this.props.isBranchOpen[arrayIndex]}><Row
                className="dashboard-branchstatistic-branch-div"></Row></Collapse>
        ));

        return rows;
    }


    renderAddScreenshot() {

        let actionButtonsDisabled = this.props.textUnitChecked.every(function (row) {
            return row.every(function (e) {
                return !e;
            })

        });

        let button = (<div style={{float: "left"}}><Button bsStyle="primary"
                                                           style={actionButtonsDisabled ? {pointerEvents: "none"} : {}}
                                                           bsSize="small" disabled={actionButtonsDisabled}
                                                           onClick={this.props.onAddScreenshotClick}>
            <FormattedMessage id="dashboard.actions.addScreenshot"/>
        </Button></div>);


        if (actionButtonsDisabled) {
            return (<OverlayTrigger placement="bottom"
                                    overlay={<Tooltip id="addscreenshot-tooltip">Select text units in the branch
                                        to upload screenhots</Tooltip>}>
                {button}
            </OverlayTrigger>);
        } else {
            return button;
        }
    }



    render() {
        // function / reuse pagination
        let previousPageButtonDisabled = DashboardStore.getState().hasNext;
        let nextPageButtonDisabled = DashboardStore.getState().hasPrevious;

        return (
            <div>
                <div className="mll mrl">
                    <div className="mtl mbl">
                        <div className="col-xs-6">
                            <ButtonToolbar>
                                {this.renderAddScreenshot()}
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

                    <Grid fluid={true} className="dashboard-branchstatistic">
                        <Row className="bms" className="dashboard-branchstatistic-header">
                            <Col md={4} className="dashboard-branchstatistic-branch-col1">
                                <FormattedMessage id="dashboard.table.header.branch"/>
                            </Col>
                            <Col md={2}>
                                <FormattedMessage id="repositories.table.header.needsTranslation"/>
                            </Col>
                            <Col md={1}>
                                <FormattedMessage id="dashboard.table.header.screenshot"/>
                            </Col>

                            <Col md={2}>
                                <FormattedMessage id="dashboard.table.header.createdBy"/>
                            </Col>

                            <Col md={2}>
                                <FormattedMessage id="dashboard.table.header.createdDate"/>
                            </Col>

                            <Col md={1}>
                                <FormattedMessage id="dashboard.table.header.deleted"/>
                            </Col>
                        </Row>

                        {this.props.branchStatistics.map(this.branch.bind(this))}
                    </Grid>
                </div>
            </div>

        );
    }
}

export default injectIntl(DashboardSearchResults);