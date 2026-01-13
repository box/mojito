import type { TextUnitSearchRequest } from '../../api/text-units';

export type WorkbenchRow = {
  id: string;
  textUnitName: string;
  repositoryName: string;
  assetPath: string | null;
  locations: string[];
  locale: string;
  localeId: number | null;
  source: string;
  translation: string | null;
  status: string;
  comment: string | null;
  tmTextUnitId: number;
  tmTextUnitVariantId: number | null;
  tmTextUnitCurrentVariantId: number | null;
  canEdit: boolean;
};

export type StatusFilterValue =
  | 'ALL'
  | 'FOR_TRANSLATION'
  | 'REVIEW_NEEDED'
  | 'TRANSLATED'
  | 'TRANSLATED_AND_NOT_REJECTED'
  | 'UNTRANSLATED'
  | 'REJECTED'
  | 'NOT_REJECTED'
  | 'APPROVED_AND_NOT_REJECTED';

export type WorkbenchDiffModalData = {
  rowId: string;
  textUnitName: string;
  repositoryName: string;
  assetPath: string | null;
  locale: string;
  baselineTarget: string | null;
  latestTarget: string | null;
  baselineStatusLabel: string;
  latestStatusLabel: string;
  baselineVariantId: number | null;
  savedVariantId: number | null;
  baselineCurrentVariantId: number | null;
  savedCurrentVariantId: number | null;
};

export type WorkbenchCollectionEntry = {
  tmTextUnitId: number;
  repositoryId: number | null;
};

export type WorkbenchCollection = {
  id: string;
  name: string;
  entries: WorkbenchCollectionEntry[];
  updatedAt: number;
};

export type WorkbenchShareOverrides = {
  searchRequest: TextUnitSearchRequest | null;
  pinnedIds?: number[];
  forceAskLocale?: boolean;
};

export type WorkbenchCollectionsState = {
  collections: WorkbenchCollection[];
  activeCollectionId: string | null;
};
