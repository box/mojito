import _ from "lodash";
import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from 'react-intl';
import ReactSidebarResponsive from "../misc/ReactSidebarResponsive";

import Screenshot from "./Screenshot";
import ScreenshotsTextUnit from "./ScreenshotsTextUnit";

class ScreenshotsGrid extends React.Component {

    static propTypes = {
        "screenshotsData": PropTypes.array.isRequired,
        "selectedScreenshotIdx": PropTypes.number,
        "onScreenshotsTextUnitTargetClick": PropTypes.func.isRequired,
        "onScreenshotsTextUnitNameClick": PropTypes.func.isRequired,
        "onScreenshotClicked": PropTypes.func.isRequired,
        "onLocaleClick": PropTypes.func.isRequired,
        "onNameClick": PropTypes.func.isRequired,
        "onStatusGlyphClick": PropTypes.func.isRequired,
        "onStatusChanged": PropTypes.func.isRequired,
        "statusGlyphDisabled": PropTypes.bool.isRequired,
    }

    getSelectedScreenshot() {
        let selectedScreenshot = null;

        if (this.props.screenshotsData.length > 0 && this.props.selectedScreenshotIdx < this.props.screenshotsData.length) {
            selectedScreenshot = this.props.screenshotsData[this.props.selectedScreenshotIdx];
        }

        return selectedScreenshot;
    }

    getTextUnitsForSelectedScreenshot() {

        let textUnitsRendered = [];
        let selectedScreenshot = this.getSelectedScreenshot();

        if (selectedScreenshot && selectedScreenshot.textUnits) {
            textUnitsRendered = _.sortBy(selectedScreenshot.textUnits, 'id');
        }

        return textUnitsRendered;
    }

    renderSideBar() {

        return (<div>
            {
                this.getTextUnitsForSelectedScreenshot().map(textUnit => {

                    let locale = this.getSelectedScreenshot().locale.bcp47Tag;

                    return <ScreenshotsTextUnit
                        key={textUnit.id}
                        textUnit={textUnit}
                        onNameClick={(e) => this.props.onScreenshotsTextUnitNameClick(e, textUnit, locale)}
                        onTargetClick={(e) => this.props.onScreenshotsTextUnitTargetClick(e, textUnit, locale)}
                        />
            })
            }
        </div>);
    }

    renderScreenshots() {
        return this.props.screenshotsData.map((screenshot, idx) =>
            <Screenshot
                key={screenshot.name + '_' + idx}
                screenshot={screenshot}
                isSelected={idx === this.props.selectedScreenshotIdx}
                onClick={() => this.props.onScreenshotClicked(idx)}
                onLocaleClick={ () => this.props.onLocaleClick([screenshot.locale.bcp47Tag])}
                onNameClick={() => this.props.onNameClick(screenshot.name)}
                onStatusGlyphClick={() => this.props.onStatusGlyphClick(idx)}
                onStatusChanged={(status) => this.props.onStatusChanged({status: status, idx: idx})}
                statusGlyphDisabled={this.props.statusGlyphDisabled}
                />)
    }

    renderNoResults() {

        let divStyle = {clear: 'both'};

        return (
                <div style={divStyle} className="empty-search-container text-center center-block">
                    <FormattedMessage id="search.result.empty"/>
                    <img className="empty-search-container-img" src={require('../../../img/magnifying-glass.svg')} />
                </div>
        );
    }

    renderWithResults() {

        return (
                <div>
                    <ReactSidebarResponsive
                        ref="sideBarScreenshot"
                        sidebar={this.renderSideBar()}
                        rootClassName="side-bar-root-container-screenshot"
                        sidebarClassName="side-bar-container-screenshot"
                        contentClassName="side-bar-main-content-container-screenshot"
                        docked={true} pullRight={true} transitions={false}>

                        {this.renderScreenshots()}

                    </ReactSidebarResponsive>
                </div>);
    }

    renderSpinner() {
        return (
          <div class="branch-spinner mtl mbl">
            <span className="glyphicon glyphicon-refresh spinning" />
          </div>
        );
    }

    /**
     * @return {JSX}
     */
    render() {
        var res;

        if (this.props.searching) {
            res = this.renderSpinner();
        } else if (this.props.screenshotsData.length > 0) {
            res = this.renderWithResults();
        } else {
            res =this.renderNoResults();
        }

        return res;
    }
}

export default injectIntl(ScreenshotsGrid);
