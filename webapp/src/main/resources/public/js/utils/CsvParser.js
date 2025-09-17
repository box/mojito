const DOUBLE_QUOTE = '"';
const COMMA = ',';
const CARRIAGE_RETURN = '\r';
const NEW_LINE = '\n';

function stripBom(content) {
    if (!content) {
        return content;
    }
    if (content.charCodeAt(0) === 0xFEFF) {
        return content.slice(1);
    }
    return content;
}

export function parseCsv(content) {
    const rows = [];
    if (!content) {
        return rows;
    }

    const sanitized = stripBom(content);
    let current = '';
    let row = [];
    let inQuotes = false;

    for (let i = 0; i < sanitized.length; i++) {
        const char = sanitized[i];

        if (char === DOUBLE_QUOTE) {
            if (inQuotes && sanitized[i + 1] === DOUBLE_QUOTE) {
                current += DOUBLE_QUOTE;
                i += 1;
            } else {
                inQuotes = !inQuotes;
            }
            continue;
        }

        if (char === COMMA && !inQuotes) {
            row.push(current);
            current = '';
            continue;
        }

        if ((char === NEW_LINE || char === CARRIAGE_RETURN) && !inQuotes) {
            if (char === CARRIAGE_RETURN && sanitized[i + 1] === NEW_LINE) {
                i += 1;
            }
            row.push(current);
            rows.push(row);
            row = [];
            current = '';
            continue;
        }

        current += char;
    }

    if (current !== '' || row.length) {
        row.push(current);
        rows.push(row);
    }

    return rows;
}

export function csvToObjects(content) {
    const rows = parseCsv(content);
    if (!rows.length) {
        return [];
    }

    const headers = rows[0].map(header => header != null ? header.trim() : '');
    const objects = [];

    for (let rowIndex = 1; rowIndex < rows.length; rowIndex++) {
        const row = rows[rowIndex];
        const isEmptyRow = row.every(cell => (cell == null || cell.trim() === ''));
        if (isEmptyRow) {
            continue;
        }
        const obj = {};
        headers.forEach((header, columnIndex) => {
            if (!header) {
                return;
            }
            obj[header] = row[columnIndex] !== undefined ? row[columnIndex] : '';
        });
        obj.__rowNumber = rowIndex + 1;
        objects.push(obj);
    }

    return objects;
}
