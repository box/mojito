import React from "react";
import PropTypes from 'prop-types';
import {FormattedDate, FormattedMessage, FormattedNumber, injectIntl} from "react-intl";
import {
    Button,
    Col,
    Collapse,
    DropdownButton,
    Glyphicon,
    Grid,
    Label,
    MenuItem,
    OverlayTrigger,
    Row,
    Tooltip
} from "react-bootstrap";
import {Link, withRouter} from "react-router";
import ClassNames from "classnames";
import {withAppConfig} from "../../utils/AppConfig";
import LinkHelper from "../../utils/LinkHelper";
import Paginator from "../widgets/Paginator";
import AltContainer from "alt-container";
import BranchTextUnitsPaginatorStore from "../../stores/branches/BranchTextUnitsPaginatorStore";
import BranchTextUnitsPaginatorActions from "../../actions/branches/BranchTextUnitsPaginatorActions";
import BranchTextUnitsPageActions from "../../actions/branches/BranchTextUnitsPageActions";
import BranchesPageActions from "../../actions/branches/BranchesPageActions";


class BranchesSearchResults extends React.Component {

    static propTypes = {
        "branchesStore": PropTypes.object.isRequired,
        "branchTextUnitsStore": PropTypes.object.isRequired,
        "onChangeOpenBranchStatistic": PropTypes.func.isRequired,
        "onChangeSelectedBranchTextUnits": PropTypes.func.isRequired,
        "onShowBranchScreenshotsClick": PropTypes.func.isRequired,
        "onNeedTranslationClick": PropTypes.func.isRequired,
        "onTextUnitNameClick": PropTypes.func.isRequired,
    };

    isBranchStatisticOpen(branchStatistic) {
        return this.props.branchesStore.openBranchStatisticId === branchStatistic.id;
    }

    renderScreenshotPreview(branchStatistic) {

        let numberOfScreenshots =
            this.props.branchesStore.textUnitsWithScreenshotsByBranchStatisticId[branchStatistic.id].size;
        let { textUnitTotalCount : expectedNumberOfScreenshots} = branchStatistic;
        let needScreenshot = Math.max(expectedNumberOfScreenshots - numberOfScreenshots, 0);

        return <div>
            {
                needScreenshot === 0 ?
                    <Label bsStyle="success" className="mrs">
                        <FormattedMessage id="branches.done"/>
                    </Label>
                    :
                    <FormattedNumber value={needScreenshot}/>
            }
        </div>;
    }

    renderGridHeader() {
        return (
            <Row className="bms" className="branches-branchstatistic-header">
                <Col md={4} className="branches-branchstatistic-col1">
                    <FormattedMessage id="branches.header.branch"/>
                </Col>
                <Col md={2}>
                    <FormattedMessage id="branches.header.needsTranslation"/>
                </Col>
                <Col md={2}>
                    <FormattedMessage id="branches.header.screenshots"/>
                </Col>
                <Col md={2}>
                    <FormattedMessage id="branches.header.createdBy"/>
                </Col>
                <Col md={2}>
                    <FormattedMessage id="branches.header.createdDate"/>
                </Col>
            </Row>
        );
    }

    renderBranchTextUnitsPagination() {
        const branchTextUnitsPaginatorState = BranchTextUnitsPaginatorStore.getState();
        let pageSizes = [];
        for (let i of [10, 25, 50, 100]) {
            pageSizes.push(
                <MenuItem
                    key={i}
                    eventKey={i}
                    active={i === branchTextUnitsPaginatorState.limit}
                    onSelect={(s, _) => {
                        BranchTextUnitsPaginatorActions.changePageSize(s);
                        BranchTextUnitsPageActions.getBranchTextUnits();
                    }
                }>
                    {i}
                </MenuItem>
            );
        }
        const title = <FormattedMessage values={{"pageSize": branchTextUnitsPaginatorState.limit}} id="search.unitsPerPage" />;
        return (
            <Row>
                <div className="pull-right" style={{display: "flex", alignItems: "center", "gap": "15px"}}>
                    <DropdownButton id="branch-text-units-per-page" title={title}>
                        {pageSizes}
                    </DropdownButton>
                    <AltContainer store={BranchTextUnitsPaginatorStore}>
                        <Paginator
                            onPreviousPageClicked={() => {
                                BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                                BranchTextUnitsPaginatorActions.goToPreviousPage();
                                BranchTextUnitsPageActions.getBranchTextUnits();
                            }}
                            onNextPageClicked={() => {
                                BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                                BranchTextUnitsPaginatorActions.goToNextPage();
                                BranchTextUnitsPageActions.getBranchTextUnits();
                            }}/>
                    </AltContainer>
                </div>
            </Row>
        );
    }

    renderBranchStatistic(branchStatistic) {
        let rows = [];

        rows.push(this.renderBranchStatisticSummary(branchStatistic));

        if (branchStatistic.isPaginated === false){
            branchStatistic.branchTextUnitStatistics.map((branchTextUnitStatistic) => {
                rows.push(this.renderBranchTextUnitStatistic(branchStatistic, branchTextUnitStatistic));
            });
        } else {
            if (this.isBranchStatisticOpen(branchStatistic)) {
                rows.push(this.renderBranchTextUnitsPagination());
            }
            const { branchTextUnitStatistics } = this.props.branchTextUnitsStore;
            branchTextUnitStatistics.map((branchTextUnitStatistic) => {
                rows.push(this.renderBranchTextUnitStatistic(branchStatistic, branchTextUnitStatistic));
            });
        }

        rows.push(this.renderBranchStatisticSeparator(branchStatistic));

        return rows;
    }

    getPullRequestUrlTemplate = (branch) => {
        try {
            return this.props.appConfig.link[branch.repository.name].pullRequest.url;
        } catch (e) {
            return null;
        }
    };

    hasScreenshot(branchStatistic) {
        return this.props.branchesStore.textUnitsWithScreenshotsByBranchStatisticId[branchStatistic.id].size > 0;
    }

    renderBranchName(branch) {
        let renderedBranchName;

        let pullRequestUrlTemplate = this.getPullRequestUrlTemplate(branch);
        renderedBranchName = LinkHelper.renderLinkOrLabel(pullRequestUrlTemplate, branch.name, {
            branchName: branch.name
        });

        return renderedBranchName;
    }

    renderOpenModalScreenshotButton(branchStatistic) {

        let disabled = !this.hasScreenshot(branchStatistic);

        // we use this construct instead of putting the button in the overlay because tooltips don't work on disabled buttons
        let button = (
            <div style={{display: "inline-block"}}>
                <Button bsStyle="default"
                        style={disabled ? {pointerEvents: "none"} : {}}
                        bsSize="small" disabled={disabled}
                        onClick={() => this.props.onShowBranchScreenshotsClick(branchStatistic)}>
                    <Glyphicon className="branches-branchstatistic-col1-screenshot" glyph="picture"/>
                </Button>
            </div>
        );

        button = (
            <OverlayTrigger placement="bottom"
                            overlay={<Tooltip id="BranchesSearchResults.tooltip.screenshot">
                                <FormattedMessage
                                    id={disabled ? "branches.searchResults.tooltip.noscreenshots" : "branches.searchResults.tooltip.withscreenshots"}/>
                            </Tooltip>}>
                {button}
            </OverlayTrigger>);


        return <div className="branches-branchstatistic-screenshotpreview">{button}</div>;
    }

    renderBranchStatisticSummary(branchStatistic) {

        let isBranchStatisticOpen = this.isBranchStatisticOpen(branchStatistic);

        return (
            <Row key={"branchStatistic-" + branchStatistic.id} className="branches-branchstatistic-summary">
                <Col md={4} className="branches-branchstatistic-col1">
                    <Row className="branches-branchstatistic-col1-row">
                        <Col md={4}>
                            <Button bsSize="xsmall"
                                    onClick={() =>
                                        this.props.onChangeOpenBranchStatistic(isBranchStatisticOpen ? null : branchStatistic)
                                    }>
                                <Glyphicon glyph={isBranchStatisticOpen ? "chevron-down" : "chevron-right"}
                                           className="color-gray-light"/>
                            </Button>
                            <span className="mlm">
                                {this.renderBranchName(branchStatistic.branch)}
                            </span>
                        </Col>
                        <Col md={4}>
                            <span className="mls color-gray-light2"><small>{branchStatistic.branch.repository.name}</small></span>
                        </Col>
                        <Col md={2}>
                            {branchStatistic.branch.deleted ?
                                <Label bsStyle="light">
                                    <FormattedMessage id="branches.deleted"/>
                                </Label>
                                :
                                ""
                            }
                        </Col>
                        <Col md={2}>
                            {this.renderOpenModalScreenshotButton(branchStatistic)}
                        </Col>
                    </Row>

                </Col>
                <Col md={2}>
                    <Link className="clickable"
                          onClick={() => this.props.onNeedTranslationClick(branchStatistic, null, branchStatistic.forTranslationCount > 0)}>
                        {branchStatistic.forTranslationCount > 0 ?
                            <FormattedNumber value={branchStatistic.forTranslationCount}/>
                            :
                            <Label bsStyle="success" className="mrs">
                                <FormattedMessage id="branches.done"/>
                            </Label>
                        }
                    </Link>
                </Col>
                <Col md={2}>
                    {this.renderScreenshotPreview(branchStatistic)}
                </Col>
                <Col md={2}>
                    <span>{branchStatistic.branch.createdByUser ? branchStatistic.branch.createdByUser.username : "-"}</span>
                </Col>
                <Col md={2}>
                    <span><FormattedDate value={branchStatistic.branch.createdDate} day="numeric" month="numeric"
                                         year="numeric"/></span>
                </Col>
            </Row>
        );
    }

    renderBranchTextUnitStatistic(branchStatistic, branchTextUnitStatistic) {

        let isTextUnitChecked =
            this.props.branchesStore.selectedBranchTextUnitIds.indexOf(branchTextUnitStatistic.id) !== -1;

        let className = ClassNames("branches-branchstatistic-textunit", {"branches-branchstatistic-textunit-open": this.isBranchStatisticOpen(branchStatistic)});

        return (<Collapse in={this.isBranchStatisticOpen(branchStatistic)} key={"branchStatisticTextUnit-" + branchTextUnitStatistic.id}>
            <div>
                <Row className={className}>

                    <Col md={4} className="branches-branchstatistic-col1">
                        <div>
                            <div className="branches-branchstatistic-textunit-check">
                                <input
                                    type="checkbox"
                                    checked={isTextUnitChecked}
                                    onClick={(e) => {
                                        let newSelected = this.props.branchesStore.selectedBranchTextUnitIds.slice();

                                        let index = newSelected.indexOf(branchTextUnitStatistic.id);

                                        if (index !== -1) {
                                            newSelected.splice(index, 1);
                                        } else {
                                            newSelected.push(branchTextUnitStatistic.id);
                                        }

                                        this.props.onChangeSelectedBranchTextUnits(newSelected);
                                    }}/>
                            </div>
                            <div className="plm">
                                <Link className="clickable"
                                      onClick={() => {this.props.onTextUnitNameClick(branchStatistic, branchTextUnitStatistic.tmTextUnit.id);}}>
                                    {branchTextUnitStatistic.tmTextUnit.name}
                                </Link>
                            </div>
                            <div
                                className="branches-branchstatistic-textunit-content">{branchTextUnitStatistic.tmTextUnit.content}
                            </div>
                        </div>
                    </Col>
                    <Col md={2}>
                        <div>
                            {branchTextUnitStatistic.forTranslationCount > 0 &&
                            <Link className="clickable"
                                  onClick={() => this.props.onNeedTranslationClick(
                                      branchStatistic,
                                      branchTextUnitStatistic.tmTextUnit.id,
                                      branchTextUnitStatistic.forTranslationCount > 0)}
                            >
                                <FormattedNumber value={branchTextUnitStatistic.forTranslationCount}/>
                            </Link>
                            }
                        </div>
                    </Col>
                    <Col md={2}>
                        <div>
                            {this.props.branchesStore.textUnitsWithScreenshotsByBranchStatisticId[branchStatistic.id].has(branchTextUnitStatistic.tmTextUnit.id) ?
                                "" :
                                "1"}
                        </div>
                    </Col>
                </Row>
            </div>
        </Collapse>);
    }

    renderBranchStatisticSeparator(branchStatistic) {
        return (
            <Collapse key={`renderBranchStatisticSeparator-${branchStatistic.id}`} in={this.isBranchStatisticOpen(branchStatistic)}>
                <Row className="mbl"></Row>
            </Collapse>
        );
    }

    render() {
        return (
            <div>
                <div className="mll mrl" className="branches-branchstatistic">
                    <Grid fluid={true}>
                        {this.renderGridHeader()}
                        {this.props.branchesStore.branchStatistics.map(this.renderBranchStatistic.bind(this))}
                    </Grid>
                </div>
            </div>
        );
    }
}

export default withAppConfig(withRouter(injectIntl(BranchesSearchResults)));