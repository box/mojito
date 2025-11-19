import BaseClient from "./BaseClient";
import TextUnit from "./TextUnit";
import TextUnitIntegrityCheckRequest from "./textunit/TextUnitIntegrityCheckRequest";
import TextUnitIntegrityCheckResult from "./textunit/TextUnitIntegrityCheckResult";
import PollableTaskClient from "./PollableTaskClient";

const ASYNC_SEARCH_POLL_INTERVAL_MS = 1000;
const ASYNC_SEARCH_POLL_TIMEOUT_FALLBACK_MS = 60000;

class TextUnitClient extends BaseClient {

    /**
     * Gets the text units that matches the searcher parameters.
     *
     * Uses an HTTP POST to suppor larger number of parameters
     *
     * @param {TextUnitSearcherParameters} textUnitSearcherParameters
     *
     * @returns {Promise.<TextUnit[]|err>} a promise that retuns an array of text units
     */
    getTextUnits(textUnitSearcherParameters) {
        return this.post(this.getUrl() + '/search-hybrid', textUnitSearcherParameters.getParams()).then((result) => {
            if (result && result.results) {
                return TextUnit.toTextUnits(result.results);
            }

            if (result && result.pollingToken) {
                return this.pollAsyncSearchResult(
                    result.pollingToken.requestId,
                    result.pollingToken.recommendedPollingDurationMillis
                );
            }

            throw new Error('Unexpected response from async text unit search');
        });
    }

    /**
     * Deletes the current text unit.
     *
     * @param {TextUnit} textUnit
     * @returns {Promise}
     */
    deleteCurrentTranslation(textUnit) {
        return this.delete(this.getUrl(textUnit.getTmTextUnitCurrentVariantId())).then(function () {
            textUnit.setTarget(null);
            textUnit.setTranslated(false);
            return textUnit;
        });
    }

    /**
     * Saves a TextUnit.
     *
     * @param {TextUnit} textUnit
     * @returns {Promise<TextUnit, err>} a promise that returns the updated or created text unit
     */
    saveTextUnit(textUnit) {
        return this.post(this.getUrl(), textUnit.data).then(function (jsonTextUnit) {
            return TextUnit.toTextUnit(jsonTextUnit);
        });
    }

    /**
     * Saves a TextUnit.
     *
     * @param {TextUnit} textUnit
     * @returns {Promise<TextUnitIntegrityCheckResult, err>} a promise that returns the updated or created text unit
     */
    checkTextUnitIntegrity(textUnit) {
        let request = new TextUnitIntegrityCheckRequest();
        request.content = textUnit.getTarget();
        request.tmTextUnitId = textUnit.getTmTextUnitId();

        return this.post(this.getUrl() + '/check', request).then(function (jsonTextUnit) {
            return new TextUnitIntegrityCheckResult(jsonTextUnit);
        });
    }

    /**
     * Saves a VirtualAssetTextUnit build of TextUnit information.
     *
     * @param {TextUnit} textUnit
     * @returns
     */
    saveVirtualAssetTextUnit(textUnit) {
        return this.post(this.getAssetTextUnitsUrl(textUnit.getAssetId()), [{
            name: textUnit.getName(),
            content: textUnit.getSource(),
            comment: textUnit.getComment(),
            pluralForm: textUnit.getPluralForm(),
            pluralFormOther: textUnit.getPluralFormOther(),
            doNotTranslate: textUnit.getDoNotTranslate(),
        }]).then(function (pollableTask) {
            return PollableTaskClient.waitForPollableTaskToFinish(pollableTask.id).then(function (pollableTask) {
                if (pollableTask.errorMessage) {
                    throw new Error(pollableTask.errorMessage);
                }

                return textUnit;
            });
        });
    }

    importTextUnitsBatch(importPayload) {
        return this.post(this.getUrl() + 'Batch', importPayload).then(function (pollableTask) {
            return PollableTaskClient.waitForPollableTaskToFinish(pollableTask.id).then(function (resolvedTask) {
                if (resolvedTask.errorMessage) {
                    throw new Error(resolvedTask.errorMessage);
                }
                return resolvedTask;
            });
        });
    }

    getAssetTextUnitsUrl(assetId) {
        return this.baseUrl + 'virtualAssets/' + assetId + '/textUnits';
    }

    /**
     * Gets the GitBlameWithUsage that matches the given textUnit.
     * @param textUnit
     * @returns {Promise}
     */
    getGitBlameInfo(textUnit) {
        return this.get(this.getUrl() + "/gitBlameWithUsages", {"tmTextUnitId": textUnit.getTmTextUnitId()});
    }

    getTranslationHistory(textUnit) {
        return this.get(this.getUrl(textUnit.getTmTextUnitId()) + "/history", {
            "bcp47Tag": textUnit.getTargetLocale()
        });
    }

    getAiReview(textUnit) {
        return this.get(this.baseUrl + "proto-ai-review-single-text-unit", {
            "tmTextUnitVariantId": textUnit.getTmTextUnitVariantId()
        });
    }

    getEntityName() {
        return 'textunits';
    }

    pollAsyncSearchResult(requestId, recommendedPollingDurationMillis) {
        const startedAt = Date.now();
        const pollUntilMs = startedAt + (recommendedPollingDurationMillis || ASYNC_SEARCH_POLL_TIMEOUT_FALLBACK_MS);
        const pollIntervalMs = ASYNC_SEARCH_POLL_INTERVAL_MS;

        const poll = async () => {
            for (;;) {
                if (Date.now() > pollUntilMs) {
                    throw new Error('Async search request timed out');
                }

                const response = await this.fetchAsyncSearchResult(requestId);

                if (response && response.results) {
                    return TextUnit.toTextUnits(response.results);
                }

                if (response && response.pollingToken) {
                    await this.delay(pollIntervalMs);
                    continue;
                }

                if (response && response.error) {
                    const errorMessage = response.error.message || 'Async search failed';
                    throw new Error(errorMessage);
                }

                throw new Error('Unexpected async search response');
            }
        };

        return poll();
    }

    fetchAsyncSearchResult(requestId) {
        return this.buildHeaders('GET').then(headers =>
            fetch(this.getUrl() + '/search-hybrid/results/' + requestId, {
                follow: 0,
                credentials: this.getCredentialsMode(),
                headers: headers
            }).then(response => {
                // we don't call baseClient.checkStatus because it would throw on 400, etc
                if (response.status === 401) {
                    BaseClient.authenticateHandler();
                }
                return response.json().then(body => {
                    if (!response.ok && body && body.error) {
                        const errorMessage = body.error.message || 'Async search failed';
                        throw new Error(errorMessage);
                    }
                    if (!response.ok) {
                        throw new Error('Async search failed with status ' + response.status);
                    }
                    return body;
                });
            })
        );
    }

    delay(durationMs) {
        return new Promise(resolve => setTimeout(resolve, durationMs));
    }
}
;

export default new TextUnitClient();
