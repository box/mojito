import { validateCronExpression } from '../utils/CronExpressionHelper.js';

describe('validateCronExpression', () => {
  it('returns true for valid cron expressions', () => {
    expect(validateCronExpression('0 0/5 * * * ?')).toBe(true, 'every 5 minutes');
    expect(validateCronExpression('0 0 * * * ?')).toBe(true, 'every hour');
    expect(validateCronExpression('0 0 0 * * ?')).toBe(true, 'every day at midnight (UTC)');
  });

  it('returns false for invalid cron expressions', () => {
    expect(validateCronExpression('')).toBe(false);
    expect(validateCronExpression('0 0 12 * *')).toBe(false, 'cron expression must have 6 fields');
    expect(validateCronExpression('0 0 12 * * ? extra')).toBe(false, 'cron expression must have 6 fields');
    expect(validateCronExpression('invalid cron')).toBe(false);
    expect(validateCronExpression('60 0 12 * * ?')).toBe(false, '60 is not a valid value for seconds');
    expect(validateCronExpression('0 60 12 * * ?')).toBe(false, '60 is not a valid value for minutes');
    expect(validateCronExpression('0 0 24 * * ?')).toBe(false, '24 is not a valid value for hours');
    expect(validateCronExpression('0 0 12 32 * ?')).toBe(false, '32 is not a valid value for day of month');
    expect(validateCronExpression('0 0 12 * 12 ?')).toBe(false, '13 is not a valid value for month (should be 0-11 or JAN-DEC)');
    expect(validateCronExpression('0 0 12 * * 8')).toBe(false, '8 is not a valid value for day of week (should be 1-7 or SUN-SAT)');
  });

  it('returns true for wildcards', () => {
    expect(validateCronExpression('* * * * * *')).toBe(true);
  });
});