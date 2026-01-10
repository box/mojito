import type { InfiniteData } from '@tanstack/react-query';

import type { ApiTextUnit, TextUnitSearchRequest } from '../../api/text-units';
import { WORKSET_SIZE_DEFAULT, WORKSET_SIZE_MIN } from './workbench-constants';
import type { WorkbenchRow } from './workbench-types';

export const clampWorksetSize = (value: number) => {
  if (!Number.isFinite(value)) {
    return WORKSET_SIZE_DEFAULT;
  }
  return Math.max(WORKSET_SIZE_MIN, value);
};

export function updateInfiniteData<T>(
  data: InfiniteData<T[], number> | undefined,
  updateItem: (item: T) => T,
): InfiniteData<T[], number> | undefined {
  if (!data) {
    return data;
  }
  return {
    pageParams: data.pageParams,
    pages: data.pages.map((page) => page.map(updateItem)),
  };
}

export function serializeSearchRequest(request: TextUnitSearchRequest): string {
  // Normalize array ordering so equality checks are stable.
  const normalized: TextUnitSearchRequest = {
    ...request,
    repositoryIds: [...request.repositoryIds].sort((a, b) => a - b),
    localeTags: [...request.localeTags].sort((a, b) => a.localeCompare(b)),
  };

  // Keep only the fields that affect search semantics and results.
  const stable = {
    repositoryIds: normalized.repositoryIds,
    localeTags: normalized.localeTags,
    searchAttribute: normalized.searchAttribute,
    searchType: normalized.searchType,
    searchText: normalized.searchText,
    statusFilter: normalized.statusFilter ?? null,
    usedFilter: normalized.usedFilter ?? null,
    doNotTranslateFilter:
      typeof normalized.doNotTranslateFilter === 'boolean' ? normalized.doNotTranslateFilter : null,
    tmTextUnitCreatedBefore: normalized.tmTextUnitCreatedBefore ?? null,
    tmTextUnitCreatedAfter: normalized.tmTextUnitCreatedAfter ?? null,
    limit: normalized.limit ?? null,
    offset: normalized.offset ?? null,
  };

  return JSON.stringify(stable);
}

export function mapApiTextUnitToRow(textUnit: ApiTextUnit): WorkbenchRow {
  const translation = textUnit.target ?? null;
  const locations =
    textUnit.assetTextUnitUsages
      ?.split(',')
      .map((value) => value.trim())
      .filter(Boolean) ?? [];
  return {
    id: `${textUnit.tmTextUnitId}:${textUnit.targetLocale}`,
    textUnitName: textUnit.name,
    repositoryName: textUnit.repositoryName ?? '',
    assetPath: textUnit.assetPath ?? null,
    locations,
    locale: textUnit.targetLocale,
    localeId: textUnit.localeId ?? null,
    source: textUnit.source ?? '',
    translation,
    // Untranslated entries don't have a meaningful status yet (legacy workbench semantics).
    status:
      translation === null ? '' : formatStatus(textUnit.status, textUnit.includedInLocalizedFile),
    comment: textUnit.comment ?? null,
    tmTextUnitId: textUnit.tmTextUnitId,
    tmTextUnitVariantId: textUnit.tmTextUnitVariantId ?? null,
    tmTextUnitCurrentVariantId: textUnit.tmTextUnitCurrentVariantId ?? null,
  };
}

export function formatStatus(status?: string | null, includedInLocalizedFile?: boolean) {
  if (includedInLocalizedFile === false) {
    return 'Rejected';
  }

  if (!status) {
    return 'To translate';
  }

  switch (status) {
    case 'APPROVED':
      return 'Accepted';
    case 'REVIEW_NEEDED':
      return 'To review';
    case 'TRANSLATION_NEEDED':
      return 'To translate';
    default:
      // Defensive fallback: backend can evolve, but we still want a stable UI label.
      return 'To translate';
  }
}

export function mapUiStatusToApi(
  status: string,
): { status: ApiTextUnit['status']; includedInLocalizedFile: boolean } | null {
  switch (status) {
    case 'Accepted':
      return { status: 'APPROVED', includedInLocalizedFile: true };
    case 'To review':
      return { status: 'REVIEW_NEEDED', includedInLocalizedFile: true };
    case 'To translate':
      return { status: 'TRANSLATION_NEEDED', includedInLocalizedFile: true };
    case 'Rejected':
      // "Rejected" is represented as excluded from localized files, regardless of variant status.
      return { status: 'TRANSLATION_NEEDED', includedInLocalizedFile: false };
    default:
      return null;
  }
}
