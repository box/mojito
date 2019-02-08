import React from "react";
import PropTypes from 'prop-types';
import {FormattedDate, FormattedMessage, FormattedNumber, injectIntl} from "react-intl";
import {
    Button,
    ButtonToolbar,
    Col,
    Collapse,
    Glyphicon,
    Grid,
    Label,
    OverlayTrigger,
    Row,
    Tooltip
} from "react-bootstrap";
import {Link, withRouter} from "react-router";
import DashboardStore from "../../stores/dashboard/DashboardStore";
import RepositoryStore from "../../stores/RepositoryStore";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchConstants from "../../utils/SearchConstants";
import ClassNames from "classnames";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";


class DashboardSearchResults extends React.Component {

    static propTypes = {
        "currentPageNumber": PropTypes.number.isRequired,
        "branchStatistics": PropTypes.array.isRequired,
        "isBranchOpen": PropTypes.array.isRequired,
        "textUnitChecked": PropTypes.array.isRequired,
        "onTextUnitCheckboxClick": PropTypes.func.isRequired,
        "onBranchCollapseClick": PropTypes.func.isRequired,
        "onAddScreenshotClick": PropTypes.func.isRequired,
        "onShowBranchScreenshotsClick": PropTypes.func.isRequired
    };

    /**
     * Update the Workbench search params to load the translation view for the selected repo
     *
     * @param {number} repoId
     */
    updateSearchParamsForNeedsTranslation(branchStatistic, tmTextUnitId) {
        // TODO(ja) move to page ...
        let repoIds = [branchStatistic.branch.repository.id];

        let params = {
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": repoIds,
            "branchId": branchStatistic.branch.id,
            "bcp47Tags": RepositoryStore.getAllBcp47TagsForRepositoryIds(repoIds),
            "status": SearchParamsStore.STATUS.FOR_TRANSLATION
        }

        if (tmTextUnitId != null) {
            params["tmTextUnitIds"] = [tmTextUnitId];
        }

        WorkbenchActions.searchParamsChanged(params);
        this.props.router.push("/workbench", null, null);
    }


    branch(branchStatistic, arrayIndex) {
        let rows = [];

        rows.push((this.renderBranchSummary(branchStatistic, arrayIndex)));

        branchStatistic.branchTextUnitStatistics.map((branchTextUnitStatistic, arrayIndexTextUnit) => {
            rows.push((this.renderCollapsable(branchStatistic, branchTextUnitStatistic, arrayIndex, arrayIndexTextUnit)));
        });

        rows.push((
            <Collapse in={this.props.isBranchOpen[arrayIndex]}><Row
                className="dashboard-branchstatistic-branch-div"></Row></Collapse>
        ));

        return rows;
    }

    renderBranchSummary(branchStatistic, arrayIndex) {

        let className = ClassNames("dashboard-branchstatistic-branch", {"dashboard-branchstatistic-branch-open": false && this.props.isBranchOpen[arrayIndex]});

        return (
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
                            {/* TODO(ja) remove repository stuff */}
                            <FormattedMessage id="repositories.table.row.done"/>
                        </Label>
                        :
                        <Link
                            onClick={this.updateSearchParamsForNeedsTranslation.bind(this, branchStatistic, null)}>
                                <span className="dashboard-branchstatistic-counts"><FormattedNumber
                                    value={branchStatistic.forTranslationCount}/>&nbsp;</span>/&nbsp;<FormattedNumber
                            value={branchStatistic.totalCount}/>
                        </Link>
                    }
                </Col>
                <Col md={2}>
                    {this.renderScreenshotPreview(branchStatistic, arrayIndex)}
                </Col>
                <Col md={2}>
                    <span>{branchStatistic.branch.createdByUser ? branchStatistic.branch.createdByUser.username : "-"}</span>
                </Col>
                <Col md={1}>
                    <span><FormattedDate value={branchStatistic.branch.createdDate} day="numeric" month="numeric"
                                         year="numeric"/></span>
                </Col>
                <Col md={1}>
                    <span>{branchStatistic.branch.deleted ? <FormattedMessage id="label.yes"/> :
                        <FormattedMessage id="label.no"/>}</span>
                </Col>
            </Row>
        );
    }

    renderScreenshotPreview(branchStatistic, arrayIndex) {

        let numberOfScreenshots = branchStatistic.textUnitsWithScreenshots.size;
        let expectedNumberOfScreenshots = branchStatistic.expectedNumberOfScreenshots;

        return (<div onClick={() => this.props.onShowBranchScreenshotsClick(arrayIndex)}><Link>
            <span className="dashboard-branchstatistic-counts"><FormattedNumber
                value={numberOfScreenshots}/>&nbsp;</span>/&nbsp;<FormattedNumber
            value={expectedNumberOfScreenshots}/>
        </Link>
            <Glyphicon className="dashboard-branchstatistic-preview mlm" glyph="picture"/>
        </div>);

    }

    renderCollapsable(branchStatistic, branchTextUnitStatistic, arrayIndex, arrayIndexTextUnit) {

        let className = ClassNames("dashboard-branchstatistic-branch-textunit", {"dashboard-branchstatistic-branch-open": this.props.isBranchOpen[arrayIndex]});

        return (<Collapse in={this.props.isBranchOpen[arrayIndex]}>
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
                            >
                                    <span className="dashboard-branchstatistic-counts"><FormattedNumber
                                        value={branchTextUnitStatistic.forTranslationCount}/>&nbsp;</span>/&nbsp;
                                <FormattedNumber
                                    value={branchTextUnitStatistic.totalCount}/>
                            </Link>
                        </div>
                    </Col>
                    <Col md={2}>
                        <div>
                            {branchTextUnitStatistic.tmTextUnit.screenshotUploaded ?
                                <Glyphicon glyph="ok" className="color-gray-light"/> :
                                <Glyphicon glyph="remove" className="color-gray-light"/>}
                        </div>
                    </Col>
                </Row>
            </div>
        </Collapse>);
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
                            <Col md={2}>
                                <FormattedMessage id="dashboard.table.header.screenshots"/>
                            </Col>
                            <Col md={2}>
                                <FormattedMessage id="dashboard.table.header.createdBy"/>
                            </Col>
                            <Col md={1}>
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

export default withRouter(injectIntl(DashboardSearchResults));