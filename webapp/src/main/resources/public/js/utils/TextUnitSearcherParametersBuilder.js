import SearchParamsStore from "../stores/workbench/SearchParamsStore";
import TextUnitSearcherParameters from "../sdk/TextUnitSearcherParameters";

/**
 * Builds {@link TextUnitSearcherParameters} from the current workbench search params state.
 * Mirrors logic used for the main workbench search so export/download flows stay aligned.
 *
 * @param {Object} searchParams state from {@link SearchParamsStore}
 * @returns {{textUnitSearcherParameters: TextUnitSearcherParameters, returnEmpty: boolean}}
 */
export function buildTextUnitSearcherParameters(searchParams) {
    const textUnitSearcherParameters = new TextUnitSearcherParameters();
    let returnEmpty = false;

    if (!SearchParamsStore.isReadyForSearching(searchParams)) {
        returnEmpty = true;
    }

    if (searchParams.searchText) {
        if (searchParams.searchAttribute === SearchParamsStore.SEARCH_ATTRIBUTES.SOURCE) {
            textUnitSearcherParameters.source(searchParams.searchText);
        } else if (searchParams.searchAttribute === SearchParamsStore.SEARCH_ATTRIBUTES.TARGET) {
            textUnitSearcherParameters.target(searchParams.searchText);
        } else if (searchParams.searchAttribute === SearchParamsStore.SEARCH_ATTRIBUTES.ASSET) {
            textUnitSearcherParameters.assetPath(searchParams.searchText);
        } else if (searchParams.searchAttribute === SearchParamsStore.SEARCH_ATTRIBUTES.PLURAL_FORM_OTHER) {
            textUnitSearcherParameters.pluralFormOther(searchParams.searchText);
        } else if (searchParams.searchAttribute === SearchParamsStore.SEARCH_ATTRIBUTES.TM_TEXT_UNIT_IDS) {
            const ids = searchParams.searchText
                .split(',')
                .map(value => parseInt(value.trim(), 10))
                .filter(value => !isNaN(value));

            textUnitSearcherParameters.tmTextUnitIds(ids.length ? ids : [0]);
        } else {
            textUnitSearcherParameters.name(searchParams.searchText);
        }

        textUnitSearcherParameters.searchType(searchParams.searchType.toUpperCase());
    }

    if (searchParams.status) {
        textUnitSearcherParameters.statusFilter(searchParams.status);
    }

    if (!searchParams.used && !searchParams.unUsed) {
        returnEmpty = true;
    } else if (searchParams.used && !searchParams.unUsed) {
        textUnitSearcherParameters.usedFilter(textUnitSearcherParameters.USED);
    } else if (!searchParams.used && searchParams.unUsed) {
        textUnitSearcherParameters.usedFilter(textUnitSearcherParameters.UNUSED);
    }

    if (!searchParams.translate && !searchParams.doNotTranslate) {
        returnEmpty = true;
    } else if (searchParams.translate && !searchParams.doNotTranslate) {
        textUnitSearcherParameters.doNotTranslateFilter(false);
    } else if (!searchParams.translate && searchParams.doNotTranslate) {
        textUnitSearcherParameters.doNotTranslateFilter(true);
    }

    if (searchParams.tmTextUnitCreatedBefore) {
        textUnitSearcherParameters.tmTextUnitCreatedBefore(searchParams.tmTextUnitCreatedBefore);
    }

    if (searchParams.tmTextUnitCreatedAfter) {
        textUnitSearcherParameters.tmTextUnitCreatedAfter(searchParams.tmTextUnitCreatedAfter);
    }

    if (searchParams.tmTextUnitIds != null && searchParams.tmTextUnitIds.length > 0) {
        textUnitSearcherParameters.tmTextUnitIds(searchParams.tmTextUnitIds);
    }

    if (searchParams.branchId != null) {
        textUnitSearcherParameters.branchId(searchParams.branchId);
    }

    if (searchParams.repoIds) {
        textUnitSearcherParameters.repositoryIds(searchParams.repoIds);
    }

    if (searchParams.bcp47Tags) {
        textUnitSearcherParameters.localeTags(searchParams.bcp47Tags);
    }

    return {textUnitSearcherParameters, returnEmpty};
}
