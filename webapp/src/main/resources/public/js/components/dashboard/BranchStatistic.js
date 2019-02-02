import React from "react";
import {Button, Collapse, Col, Glyphicon, Grid, Row} from "react-bootstrap";
import {Link} from "react-router";
import {FormattedMessage, FormattedNumber} from "react-intl";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchConstants from "../../utils/SearchConstants";
import PropTypes from "prop-types";
import RepositoryStore from "../../stores/RepositoryStore";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";


class BranchStatistic extends React.Component {
    static propTypes = {
        "branchStatistic": PropTypes.any.isRequired,
        "isBranchOpen": PropTypes.bool.isRequired,
        "textUnitChecked": PropTypes.array.isRequired,
        "onUploadImageClick": PropTypes.func.isRequired,
        "onTextUnitForScreenshotUploadClick": PropTypes.func.isRequired,
        "onBranchCollapseClick": PropTypes.func.isRequired,
        "onChooseImageClick": PropTypes.func.isRequired
    };

    /**
     * Update the Workbench search params to load the translation view for the selected repo
     *
     * @param {number} repoId
     */
    updateSearchParamsForNeedsTranslation(textUnitId) {

        let repoIds = [this.props.branchStatistic.branch.repository.id];

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": repoIds,
            "branchId": this.props.branchStatistic.branch.id,
            "tmTextUnitIds" : textUnitId,
            "bcp47Tags": RepositoryStore.getAllBcp47TagsForRepositoryIds(repoIds),
        });
    }

    updateScreenshotSearchParams(branchId) {
        //TODO add branchId to screenshot search params
        // ScreenshotsSearchTextActions.changeSearchText(
        //     {
        //
        //     }
        //);
    }

    createTextUnitsCollapsible(branchTextUnitStatistic, arrayIndex) {
        return (
            <Row className={arrayIndex % 2 == 0 ? "dashboard-branch-collapse-light" : "dashboard-branch-collapse-dark"}>
                <Col md={4}>
                    <input
                        type="checkbox"
                        checked={this.props.textUnitChecked[arrayIndex]}
                        onChange={() => this.props.onTextUnitForScreenshotUploadClick(arrayIndex)}/>
                    <em>{branchTextUnitStatistic.tmTextUnit.content}</em>
                </Col>
                <Col md={4}>
                    <Link
                        onClick={this.updateSearchParamsForNeedsTranslation.bind(this, branchTextUnitStatistic.tmTextUnit.id)}
                        to='/workbench'>
                        <span className="branch-counts"><FormattedNumber value={branchTextUnitStatistic.forTranslationCount}/>&nbsp;</span>
                        (&nbsp;<FormattedMessage values={{numberOfWords: branchTextUnitStatistic.totalCount}}
                                                 id="repositories.table.row.numberOfWords"/>&nbsp;)
                    </Link>
                </Col>
                <Col md={4}>
                    {branchTextUnitStatistic.tmTextUnit.screenshotUploaded ?
                        <button className='glyphicon glyphicon-ok'/> : <button className='glyphicon glyphicon-remove'/>}
                </Col>
            </Row>
        );
    }


    render() {

       return (
           <div>
               <div>
                   <Grid fluid={true}>
                       <Row>
                           <Col md={4}>
                                <Button bsSize="xsmall" onClick={() => this.props.onBranchCollapseClick()}>
                                    <Glyphicon glyph={ this.props.isBranchOpen ? "chevron-down" : "chevron-right"} className="color-gray-light" />
                                </Button>
                               <span className="mlm">{this.props.branchStatistic.branch.name}</span>
                           </Col>
                           <Col md={4}>
                               <Link
                                   onClick={this.updateSearchParamsForNeedsTranslation.bind(this, null)}
                                   to='/workbench'>
                                   <span className="branch-counts"><FormattedNumber value={this.props.branchStatistic.forTranslationCount}/>&nbsp;</span>
                                   (&nbsp;<FormattedMessage values={{numberOfWords: this.props.branchStatistic.totalCount}}
                                                            id="repositories.table.row.numberOfWords"/>&nbsp;)
                               </Link>
                           </Col>
                       </Row>
                   </Grid>
               </div>
               <div>
                   <Collapse in={this.props.isBranchOpen}>
                       <Grid fluid={true}>
                           {this.props.branchStatistic.branchTextUnitStatistics.map(this.createTextUnitsCollapsible.bind(this))}
                       </Grid>
                   </Collapse>
               </div>
           </div>
       )
    }

}

export default BranchStatistic;