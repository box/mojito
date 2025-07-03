export const validateCronExpression = (cron) => {
    if (!cron) return false;
    const fields = cron.trim().split(/\s+/);
    if (fields.length !== 6) return false;
    const fieldDefs = [
        { name: 'Seconds',   re: /^[0-5]?[0-9]([,\-*/]?[0-5]?[0-9])*$/ },
        { name: 'Minutes',   re: /^[0-5]?[0-9]([,\-*/]?[0-5]?[0-9])*$/ },
        { name: 'Hours',     re: /^([01]?\d|2[0-3])([,\-*/]?([01]?\d|2[0-3]))*$/ },
        { name: 'DayOfMonth',re: /^([1-9]|[12]\d|3[01]|\?|L|W)([,\-*/]?([1-9]|[12]\d|3[01]|\?|L|W))*$/ },
        { name: 'Month',     re: /^(0?[1-9]|1[0-1]|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)([,\-*/]?(0?[1-9]|1[0-1]|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))*$/i },
        { name: 'DayOfWeek', re: /^([1-7]|SUN|MON|TUE|WED|THU|FRI|SAT|\?|L|#)([,\-*/]?([1-7]|SUN|MON|TUE|WED|THU|FRI|SAT|\?|L|#))*$/i }
    ];
    for (let i = 0; i < 6; i++) {
        if (!fieldDefs[i].re.test(fields[i]) && fields[i] !== '*') {
            return false;
        }
    }
    return true;
};