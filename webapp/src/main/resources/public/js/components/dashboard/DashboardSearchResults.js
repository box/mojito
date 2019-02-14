import React from "react";
import PropTypes from 'prop-types';
import {FormattedDate, FormattedMessage, FormattedNumber, injectIntl} from "react-intl";
import {Button, Col, Collapse, Glyphicon, Grid, Label, Row} from "react-bootstrap";
import {Link, withRouter} from "react-router";
import RepositoryStore from "../../stores/RepositoryStore";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchConstants from "../../utils/SearchConstants";
import ClassNames from "classnames";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";


class DashboardSearchResults extends React.Component {

    static propTypes = {
        "branchStatistics": PropTypes.array.isRequired,
        "openBranchStatisticId": PropTypes.number.isRequired,
        "selectedBranchTextUnitIds": PropTypes.array.isRequired,
        "textUnitsWithScreenshotsByBranchStatisticId":  PropTypes.any.isRequired,
        "onChangeOpenBranchStatistic": PropTypes.func.isRequired,
        "onChangeSelectedBranchTextUnits": PropTypes.func.isRequired,
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


    isBranchStatisticOpen(branchStatistic) {
        return this.props.openBranchStatisticId === branchStatistic.id;
    }

    renderScreenshotPreview(branchStatistic) {

        let numberOfScreenshots = this.props.textUnitsWithScreenshotsByBranchStatisticId[branchStatistic.id].size;
        let expectedNumberOfScreenshots = branchStatistic.branchTextUnitStatistics.length;

        return (
            <div onClick={() => this.props.onShowBranchScreenshotsClick(branchStatistic.id)}>
                <Link>
                    <span className="dashboard-branchstatistic-counts"><FormattedNumber
                        value={numberOfScreenshots}/>&nbsp;</span>/&nbsp;<FormattedNumber
                    value={expectedNumberOfScreenshots}/>
                </Link>
                <Glyphicon className="dashboard-branchstatistic-preview mlm" glyph="picture"/>
            </div>
        );
    }

    renderGridHeader() {
        return (
            <Row className="bms" className="dashboard-branchstatistic-header">
                <Col md={4} className="dashboard-branchstatistic-branch-col1">
                    <FormattedMessage id="dashboard.table.header.branch"/>
                </Col>
                <Col md={2}>
                    {/*TODO(ja) rename those properties*/}
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
        );
    }

    renderBranchStatistic(branchStatistic) {
        let rows = [];

        rows.push((this.renderBranchStatisticSummary(branchStatistic)));

        branchStatistic.branchTextUnitStatistics.map((branchTextUnitStatistic) => {
            rows.push((this.renderBranchTextUnitStatistic(branchStatistic, branchTextUnitStatistic)));
        });

        //TODO(ja) clean that up
        rows.push((
            <Collapse in={this.isBranchStatisticOpen(branchStatistic)}>
                <Row className="dashboard-branchstatistic-branch-div"></Row>
            </Collapse>
        ));

        return rows;
    }

    renderBranchStatisticSummary(branchStatistic) {

        let isBranchStatisticOpen = this.isBranchStatisticOpen(branchStatistic);

        return (
            <Row key={"branchStatistic-" + branchStatistic.id} className="dashboard-branchstatistic-branch">
                <Col md={4} className="dashboard-branchstatistic-branch-col1">
                    <Button bsSize="xsmall"
                            onClick={() =>
                                this.props.onChangeOpenBranchStatistic(isBranchStatisticOpen ? null : branchStatistic.id)
                            }>
                        <Glyphicon glyph={isBranchStatisticOpen ? "chevron-down" : "chevron-right"}
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
                    {this.renderScreenshotPreview(branchStatistic)}
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

    renderBranchTextUnitStatistic(branchStatistic, branchTextUnitStatistic) {

        let isTextUnitChecked = this.props.selectedBranchTextUnitIds.indexOf(branchTextUnitStatistic.id) !== -1;

        let className = ClassNames("dashboard-branchstatistic-branch-textunit", {"dashboard-branchstatistic-branch-open": this.isBranchStatisticOpen(branchStatistic)});

        let propz = this.props;

        console.log(branchTextUnitStatistic.tmTextUnit.id in this.props.textUnitsWithScreenshotsByBranchStatisticId[branchStatistic.id]);

        return (<Collapse in={this.isBranchStatisticOpen(branchStatistic)}>
            <div>
                <Row key={"branchStatisticTextUnit-" + branchTextUnitStatistic.id} className={className}>

                    <Col md={4} className="dashboard-branchstatistic-branch-col1">
                        <div>
                            <div className="dashboard-branchstatistic-branch-col1-check">
                                <input
                                    type="checkbox"
                                    checked={isTextUnitChecked}
                                    onClick={(e) => {
                                        var newSelected = this.props.selectedBranchTextUnitIds.slice();

                                        let index = newSelected.indexOf(branchTextUnitStatistic.id);

                                        if (index !== -1) {
                                            newSelected.splice(index, 1);
                                        } else {
                                            newSelected.push(branchTextUnitStatistic.id);
                                        }

                                        this.props.onChangeSelectedBranchTextUnits(newSelected);
                                    }}/>
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
                            {this.props.textUnitsWithScreenshotsByBranchStatisticId[branchStatistic.id].has(branchTextUnitStatistic.tmTextUnit.id) ?
                                <Glyphicon glyph="ok" className="color-gray-light"/> :
                                <Glyphicon glyph="remove" className="color-gray-light"/>}
                        </div>
                    </Col>
                </Row>
            </div>
        </Collapse>);
    }

    render() {
        return (
            <div>
                <div className="mll mrl">
                    <Grid fluid={true} className="dashboard-branchstatistic">
                        {this.renderGridHeader()}
                        {this.props.branchStatistics.map(this.renderBranchStatistic.bind(this))}
                    </Grid>
                </div>
            </div>
        );
    }
}

export default withRouter(injectIntl(DashboardSearchResults));