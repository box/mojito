import React from "react";
import {Button, Col, Glyphicon, Row} from "react-bootstrap";
import {Link} from "react-router";
import {FormattedMessage, FormattedNumber} from "react-intl";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchConstants from "../../utils/SearchConstants";
import PropTypes from "prop-types";

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
            "tmTextUnitIds": textUnitId,
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

            <Row key={arrayIndex}
                 className="dashboard-branchstatistic-subrow">

                <Col md={4}>
                    <div>
                        <input
                            type="checkbox"
                            checked={this.props.textUnitChecked[arrayIndex]}
                            onChange={() => this.props.onTextUnitForScreenshotUploadClick(arrayIndex)}/>
                        <em>{branchTextUnitStatistic.tmTextUnit.content}</em>
                    </div>
                </Col>

                <Col md={4}>
                    <div>
                        <Link
                            onClick={this.updateSearchParamsForNeedsTranslation.bind(this, branchTextUnitStatistic.tmTextUnit.id)}
                            to='/workbench'>
                        <span className="branch-counts"><FormattedNumber
                            value={branchTextUnitStatistic.forTranslationCount}/>&nbsp;</span>
                            (&nbsp;<FormattedMessage
                            values={{numberOfWords: branchTextUnitStatistic.totalCount}}
                            id="repositories.table.row.numberOfWords"/>&nbsp;)
                        </Link>
                    </div>
                </Col>
                <Col md={4}>
                    <div>
                        {branchTextUnitStatistic.tmTextUnit.screenshotUploaded ?
                            <button className='glyphicon glyphicon-ok'/> :
                            <button className='glyphicon glyphicon-remove'/>}
                    </div>
                </Col>
            </Row>
        );
    }


    render() {
        return (
            <div>
                <div className="dashboard-branchstatistic-row">
                    <div>

                    </div>
                </div>
            </div>
        )
    }

}

export default BranchStatistic;