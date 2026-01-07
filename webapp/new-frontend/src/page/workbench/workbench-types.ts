export type WorkbenchRow = {
  id: string;
  textUnitName: string;
  repositoryName: string;
  assetPath: string | null;
  locale: string;
  localeId: number | null;
  source: string;
  translation: string | null;
  status: string;
  comment: string | null;
  tmTextUnitId: number;
  tmTextUnitVariantId: number | null;
  tmTextUnitCurrentVariantId: number | null;
};

export type RepositoryOption = { id: number; name: string };
export type LocaleOption = { tag: string; label: string };

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
