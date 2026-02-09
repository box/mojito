import type { DateQuickRange } from '../components/filters/MultiSectionFilterChip';

export function getStandardDateQuickRanges(): DateQuickRange[] {
  return [
    { label: 'Last 5 min', after: minutesAgoIso(5), before: null },
    { label: 'Last 10 min', after: minutesAgoIso(10), before: null },
    { label: 'Last hour', after: minutesAgoIso(60), before: null },
    { label: 'Today', after: startOfTodayIso(), before: null },
    { label: 'Yesterday', after: startOfYesterdayIso(), before: startOfTodayIso() },
    { label: 'This week', after: startOfWeekIso(), before: null },
  ];
}

function minutesAgoIso(minutes: number) {
  return new Date(Date.now() - minutes * 60 * 1000).toISOString();
}

function startOfTodayIso() {
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  return now.toISOString();
}

function startOfYesterdayIso() {
  const date = new Date();
  date.setDate(date.getDate() - 1);
  date.setHours(0, 0, 0, 0);
  return date.toISOString();
}

function startOfWeekIso() {
  const now = new Date();
  const day = now.getDay();
  const diff = (day + 6) % 7;
  now.setDate(now.getDate() - diff);
  now.setHours(0, 0, 0, 0);
  return now.toISOString();
}
