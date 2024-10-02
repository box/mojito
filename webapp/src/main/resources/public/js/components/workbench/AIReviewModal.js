import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, Glyphicon, Modal} from "react-bootstrap";
import {withAppConfig} from "../../utils/AppConfig";
import TextUnitSDK from "../../sdk/TextUnit";

class AIReviewModal extends React.Component {
    static propTypes() {
        return {
            "show": PropTypes.bool.isRequired,
            "onCloseModal": PropTypes.func.isRequired,
        };
    }

    closeModal = () => {
        this.props.onCloseModal();
    };

    getTitle = () => {
        return this.props.intl.formatMessage({id: "aiReviewModal.title"});
    };

    renderRating = (rating) => {
        let label;
        switch (rating) {
            case 0:
                label = <span className="label label-danger">bad</span>;
                break;
            case 2:
                label = <span className="label label-success">good</span>;
                break;
            default:
                label = <span className="label label-warning">average</span>;
        }

        return label;
    };

    renderHeader() {
        return <React.Fragment>
            <span className="label label-primary">{this.props.textUnit.getTargetLocale()}</span>
            <span className="name">{this.props.textUnit.getName()}</span>
        </React.Fragment>
    }

    renderHeaderRight() {
        return <React.Fragment>
            <span className="label label-lg label-default">asset: {this.props.textUnit.getAssetPath()}</span>
            <span
                className="label label-lg label-default">repo: {this.props.textUnit.getRepositoryName()}</span>
        </React.Fragment>
    }


    renderSource() {
        return <React.Fragment>{this.props.textUnit.getSource()}</React.Fragment>
    }

    renderTarget() {
        return <React.Fragment>{this.props.textUnit.getTarget()}</React.Fragment>
    }

    renderTargetStatus() {
        return <div className="status">{this.renderReviewGlyph()}</div>
    }

    renderComment() {
        return <React.Fragment>{this.props.textUnit.getComment()}</React.Fragment>
    }

    renderCommentRating() {
        return this.props.review &&
            <React.Fragment>
                <div className="ratings-right">{this.renderRating(this.props.review.descriptionRating.score)}</div>
                <div className="color-gray-light em">{this.props.review.descriptionRating.explanation}</div>
            </React.Fragment>
    }

    renderTargetRating() {
        return this.props.review &&
            <React.Fragment>
                <div className="ratings-right">{this.renderRating(this.props.review.existingTargetRating.score)}</div>
                <div className="color-gray-light em">{this.props.review.existingTargetRating.explanation}</div>
            </React.Fragment>
    }

    renderSuggestion1() {
        return this.props.review && <React.Fragment>
            <div className="suggestiona">{this.props.review.target.content} <span
                className="label label-default">{this.props.review.target.confidenceLevel}</span></div>

        </React.Fragment>
    }

    renderSuggestion2() {
        return this.props.review && <React.Fragment>
            <div className="suggestionb">{this.props.review.altTarget.content} <span
                className="label label-default">{this.props.review.altTarget.confidenceLevel}</span></div>
        </React.Fragment>
    }

    /* TODO(ja) make a component instead of copy/paste */
    renderReviewGlyph() {

        let ui = "";
        if (this.props.textUnit.isTranslated()) {

            let glyphType = "ok";
            let glyphTitle = this.props.intl.formatMessage({id: "textUnit.reviewModal.accepted"});

            if (!this.props.textUnit.isIncludedInLocalizedFile()) {

                glyphType = "alert";
                glyphTitle = this.props.intl.formatMessage({id: "textUnit.reviewModal.rejected"});

            } else if (this.props.textUnit.getStatus() === TextUnitSDK.STATUS.REVIEW_NEEDED) {

                glyphType = "eye-open";
                glyphTitle = this.props.intl.formatMessage({id: "textUnit.reviewModal.needsReview"});

            } else if (this.props.textUnit.getStatus() === TextUnitSDK.STATUS.TRANSLATION_NEEDED) {

                glyphType = "edit";
                glyphTitle = this.props.intl.formatMessage({id: "textUnit.reviewModal.translationNeeded"});
            }
            ui = (
                <Glyphicon glyph={glyphType} id="reviewStringButton" title={glyphTitle}/>
            );
        }

        return ui;
    }

    render() {
        if (!this.props.show) return null;

        return (
            <Modal className="ai-review-modal" show={this.props.show} onHide={this.closeModal}>
                <Modal.Header closeButton>
                    <Modal.Title>{this.getTitle()}</Modal.Title>
                </Modal.Header>
                <Modal.Body>

                    <div className="ai-review-root2">
                        <div className="section-title">Text Unit</div>
                        <div className="text-unit" style={{marginBottom: "30px"}}>
                            <div style={{gridArea: "header", display: "flex", gap: '10px'}}
                                 className="mtm mbm">
                                <span className="label label-info">{this.props.textUnit.getTargetLocale()}</span>
                                <span style={{flexGrow: 2}}>{this.props.textUnit.getName()}</span>
                                <span className="label label-default">{this.props.textUnit.getAssetPath()}</span>
                                <span className="label label-default">{this.props.textUnit.getRepositoryName()}</span>
                            </div>
                            <div style={{gridArea: "left"}}>
                                <div>{this.props.textUnit.getSource()}</div>
                                <div className="em color-gray-light2 mtm">{this.props.textUnit.getComment()}</div>
                            </div>
                            <div style={{gridArea: "right"}} className="right">
                                <div style={{gridArea: "target"}}>{this.renderTarget()}</div>
                                <div style={{gridArea: "status"}}>{this.renderReviewGlyph()}</div>
                            </div>
                        </div>

                        {this.props.review == null ?
                            <div className="loading">
                                {this.props.loading &&
                                    <span className="glyphicon glyphicon-refresh spinning"/>}
                            </div>
                            : <React.Fragment>
                                <div className="section-title">Target Analysis</div>
                                <div className="grid-2c">
                                    <div className="left">
                                        {this.renderRating(this.props.review.existingTargetRating.score)}
                                    </div>
                                    <div className="right">
                                        {this.props.review.existingTargetRating.explanation}
                                    </div>
                                </div>

                                <div className="section-title">Target Suggestions</div>
                                <div className="grid-2c">
                                    <div className="left">
                                 <span
                                     className="label label-default">{this.props.review.target.confidenceLevel}</span>
                                    </div>
                                    <div className="right">
                                        {this.props.review.target.content}
                                    </div>
                                </div>
                                <div className="grid-2c mtm">
                                    <div className="left">
                                 <span
                                     className="label label-default">{this.props.review.altTarget.confidenceLevel}</span>
                                    </div>
                                    <div className="right">
                                        {this.props.review.altTarget.content}
                                    </div>
                                </div>

                                <div className="section-title">Comment Analysis</div>
                                <div className="grid-2c">
                                    <div className="left">
                                        {this.renderRating(this.props.review.descriptionRating.score)}
                                    </div>
                                    <div className="right">
                                        {this.props.review.descriptionRating.explanation}
                                    </div>
                                </div>
                            </React.Fragment>
                    }
                    </div>
                        {/*<div className="ai-review-root">*/}
                        {/*    <div className="header">{this.renderHeader()}</div>*/}
                        {/*    <div className="header-right">{this.renderHeaderRight()}</div>*/}
                        {/*    <div className="source">{this.renderSource()}</div>*/}
                        {/*    <div className="target">{this.renderTarget()}</div>*/}
                        {/*    /!*<div className="target-status">{this.renderTargetStatus()}</div>*!/*/}
                        {/*    <div className="comment color-gray-light2 em">{this.renderComment()}</div>*/}
                        {/*    <div className="ratings">Comment and Target Ratings</div>*/}
                        {/*    <div className="comment-rating rating">{this.renderCommentRating()}</div>*/}
                        {/*    <div className="target-rating rating">{this.renderTargetRating()}</div>*/}
                        {/*    <div className="suggestions">Target Suggestions</div>*/}
                        {/*    <div className="suggestion1">{this.renderSuggestion1()}</div>*/}
                        {/*    <div className="suggestion2">{this.renderSuggestion2()}</div>*/}
                        {/*</div>*/}


                        {/*    <div className="sl"></div>*/}
                        {/*    <div className="s">*/}
                        {/*        <div>{this.props.textUnit.getSource()}</div>*/}
                        {/*            <div className="color-gray-light2 italic mtl">{this.props.textUnit.getComment()}</div>*/}
                        {/*    </div>*/}
                        {/*    <div className="se"></div>*/}
                        {/*    <div className="ct">{this.props.textUnit.getTarget()}</div>*/}

                        {/*    {this.props.loading ? <span className="glyphicon glyphicon-refresh spinning"/> :*/}
                        {/*        <div>*/}
                        {/*        <div className="ctdr">{this.renderRating(this.getRating())}*/}
                        {/*</div>*/}
                        {/*<div*/}
                        {/*    className="ctd color-gray-light italic">{this.props.review.existingTargetRating.explanation}</div>*/}

                        {/*<div className="b1"></div>*/}
                        {/*<div className="b2"></div>*/}
                        {/*<div className="b3"></div>*/}


                        {/*<div className="tl color-gray-light mtl">Suggestions</div>*/}
                        {/*<div className="t mtl">{this.props.review.target.content}</div>*/}
                        {/*<div*/}
                        {/*    className="te color-gray-light italic">{this.props.review.target.explanation}</div>*/}
                        {/*<div className="tr"><span*/}
                        {/*    className="label">{this.props.review.target.confidenceLevel}</span>*/}
                        {/*</div>*/}

                        {/*<div className="atl color-gray-light"></div>*/}
                        {/*<div className="at">{this.props.review.altTarget.content}</div>*/}
                        {/*<div*/}
                        {/*    className="ate color-gray-light italic">{this.props.review.altTarget.explanation}</div>*/}
                        {/*<div className="atr"><span*/}
                        {/*    className="label">{this.props.review.altTarget.confidenceLevel}</span></div>*/}

                        {/*<div className="ce">*/}
                        {/*    <div*/}
                        {/*        className="color-gray-light italic">{this.props.review.descriptionRating.explanation}</div>*/}
                        {/*</div>*/}

                        {/*<div*/}
                        {/*    className="cr">{this.renderRating(this.props.review.descriptionRating.score)}</div>*/}
                        {/*        </div>*/}
                        {/*        }*/}
                        {/*</div>*/}


                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="primary" onClick={this.closeModal}>
                        <FormattedMessage id={"textUnit.gitBlameModal.close"}/>
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }

    getRating() {
        return this.props.review.existingTargetRating.score;
    }
}

export default withAppConfig(injectIntl(AIReviewModal));
