var properties = require('properties');

module.exports = function(source) {
    if (this.cacheable) this.cacheable();
    const parsedProperties = properties.parse(source);
    const jsonString = JSON.stringify(parsedProperties);
    return `module.exports = ${jsonString}`;
}