import { getProperties } from 'properties-file'

export default (source) => {
    const parsed = getProperties(source);   // JS object
    const json = JSON.stringify(parsed);           // '{"foo":"bar"}'
    return `export default ${json};`;              // exports as JS object!
}
