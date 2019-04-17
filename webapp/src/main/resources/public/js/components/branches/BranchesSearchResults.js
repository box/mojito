import React from "react";
import PropTypes from 'prop-types';
import {FormattedDate, FormattedMessage, FormattedNumber, injectIntl} from "react-intl";
import {Button, Col, Collapse, Glyphicon, Grid, Label, Row} from "react-bootstrap";
import {Link, withRouter} from "react-router";
import ClassNames from "classnames";


class BranchesSearchResults extends React.Component {

    static propTypes = {
        "branchStatistics": PropTypes.array.isRequired,
        "openBranchStatisticId": PropTypes.number,
        "selectedBranchTextUnitIds": PropTypes.array.isRequired,
        "textUnitsWithScreenshotsByBranchStatisticId": PropTypes.any.isRequired,
        "onChangeOpenBranchStatistic": PropTypes.func.isRequired,
        "onChangeSelectedBranchTextUnits": PropTypes.func.isRequired,
        "onShowBranchScreenshotsClick": PropTypes.func.isRequired,
        "onNeedTranslationClick": PropTypes.func.isRequired
    };

    isBranchStatisticOpen(branchStatistic) {
        return this.props.openBranchStatisticId === branchStatistic.id;
    }

    renderScreenshotPreview(branchStatistic) {

        let numberOfScreenshots = this.props.textUnitsWithScreenshotsByBranchStatisticId[branchStatistic.id].size;
        let expectedNumberOfScreenshots = branchStatistic.branchTextUnitStatistics.length;
        let needScreenshot = expectedNumberOfScreenshots - numberOfScreenshots;

        return (
            <div onClick={() => this.props.onShowBranchScreenshotsClick(branchStatistic.id)} className="clickable">
                {needScreenshot === 0 ?
                    <Label bsStyle="success" className="mrs">
                        <FormattedMessage id="branches.done"/>
                    </Label>
                    :
                    <Link>
                        <FormattedNumber value={needScreenshot}/>
                    </Link>
                }

                {needScreenshot === 0 ?
                    ""
                    :
                    <Glyphicon className="branches-branchstatistic-screenshotpreview mlm" glyph="picture"/>
                }
            </div>
        );
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

    renderBranchStatistic(branchStatistic) {
        let rows = [];

        rows.push(this.renderBranchStatisticSummary(branchStatistic));

        branchStatistic.branchTextUnitStatistics.map((branchTextUnitStatistic) => {
            rows.push(this.renderBranchTextUnitStatistic(branchStatistic, branchTextUnitStatistic));
        });

        rows.push(this.renderBranchStatisticSeparator(branchStatistic));

        return rows;
    }

    renderBranchStatisticSummary(branchStatistic) {

        let isBranchStatisticOpen = this.isBranchStatisticOpen(branchStatistic);

        return (
            <Row key={"branchStatistic-" + branchStatistic.id} className="branches-branchstatistic-summary">
                <Col md={4} className="branches-branchstatistic-col1">
                    <Row>
                        <Col md={8}>
                            <Button bsSize="xsmall"
                                    onClick={() =>
                                        this.props.onChangeOpenBranchStatistic(isBranchStatisticOpen ? null : branchStatistic.id)
                                    }>
                                <Glyphicon glyph={isBranchStatisticOpen ? "chevron-down" : "chevron-right"}
                                           className="color-gray-light"/>
                            </Button>
                            <span className="mlm">{branchStatistic.branch.name}</span>
                        </Col>
                        <Col md={4}>

                            {branchStatistic.branch.deleted ?
                                <Label bsStyle="light">
                                    <FormattedMessage id="branches.deleted"/>
                                </Label>
                                :
                                ""
                            }
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

        let isTextUnitChecked = this.props.selectedBranchTextUnitIds.indexOf(branchTextUnitStatistic.id) !== -1;

        let className = ClassNames("branches-branchstatistic-textunit", {"branches-branchstatistic-textunit-open": this.isBranchStatisticOpen(branchStatistic)});

        return (<Collapse in={this.isBranchStatisticOpen(branchStatistic)}>
            <div>
                <Row key={"branchStatisticTextUnit-" + branchTextUnitStatistic.id} className={className}>

                    <Col md={4} className="branches-branchstatistic-col1">
                        <div>
                            <div className="branches-branchstatistic-textunit-check">
                                <input
                                    type="checkbox"
                                    checked={isTextUnitChecked}
                                    onClick={(e) => {
                                        let newSelected = this.props.selectedBranchTextUnitIds.slice();

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
                                className="branches-branchstatistic-textunit-content">{branchTextUnitStatistic.tmTextUnit.content}</div>
                        </div>
                    </Col>
                    <Col md={2}>
                        <div>
                            <Link className="clickable"
                                  onClick={() => this.props.onNeedTranslationClick(
                                      branchStatistic,
                                      branchTextUnitStatistic.tmTextUnit.id,
                                      branchTextUnitStatistic.forTranslationCount > 0)}
                            >
                                {branchTextUnitStatistic.forTranslationCount > 0 ?
                                    <FormattedNumber value={branchTextUnitStatistic.forTranslationCount}/>
                                    :
                                    <Glyphicon glyph="ok" className="color-gray-light"/>
                                }
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

    renderBranchStatisticSeparator(branchStatistic) {
        return (
            <Collapse in={this.isBranchStatisticOpen(branchStatistic)}>
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
                        {this.props.branchStatistics.map(this.renderBranchStatistic.bind(this))}
                    </Grid>
                </div>
            </div>
        );
    }
}

export default withRouter(injectIntl(BranchesSearchResults));