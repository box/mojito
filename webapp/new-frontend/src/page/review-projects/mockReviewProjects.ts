import type { ApiReviewProjectSummary } from '../../api/review-projects';

const now = new Date();

export const mockReviewProjects: ApiReviewProjectSummary[] = [
  {
    id: 101,
    createdDate: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 14).toISOString(),
    dueDate: new Date(now.getTime() + 1000 * 60 * 60 * 24 * 7).toISOString(),
    textUnitCount: 140,
    wordCount: 34000,
    type: 'NORMAL',
    status: 'OPEN',
    acceptedCount: 45,
    repositories: [
      { id: 1, name: 'Mobile App' },
      { id: 2, name: 'Web Dashboard' },
    ],
    locales: [
      {
        id: 10,
        bcp47Tag: 'fr-FR',
        displayName: 'French (France)',
        selectedCount: 120,
        acceptedCount: 45,
      },
    ],
    screenshotImageIds: ['home-hero', 'settings-menu'],
  },
  {
    id: 202,
    createdDate: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 3).toISOString(),
    dueDate: new Date(now.getTime() + 1000 * 60 * 60 * 24 * 2).toISOString(),
    textUnitCount: 90,
    wordCount: 18000,
    type: 'EMERGENCY',
    status: 'OPEN',
    acceptedCount: 60,
    repositories: [{ id: 3, name: 'Admin Console' }],
    locales: [
      { id: 11, bcp47Tag: 'ja-JP', displayName: 'Japanese', selectedCount: 80, acceptedCount: 60 },
      { id: 12, bcp47Tag: 'ko-KR', displayName: 'Korean', selectedCount: 80, acceptedCount: 60 },
    ],
  },
  {
    id: 303,
    createdDate: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 35).toISOString(),
    dueDate: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 5).toISOString(),
    closeReason: 'Completed initial rollout',
    textUnitCount: 50,
    wordCount: 8200,
    type: 'TERMINOLOGY',
    status: 'CLOSED',
    acceptedCount: 50,
    repositories: [{ id: 4, name: 'Docs' }],
    locales: [
      {
        id: 13,
        bcp47Tag: 'es-ES',
        displayName: 'Spanish (Spain)',
        selectedCount: 50,
        acceptedCount: 50,
      },
      { id: 14, bcp47Tag: 'de-DE', displayName: 'German', selectedCount: 50, acceptedCount: 50 },
    ],
    screenshotImageIds: [],
  },
];
