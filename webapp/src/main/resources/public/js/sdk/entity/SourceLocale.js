export default class SourceLocale {
    constructor() {
        this.id = null;
    }

    static toSourceLocale(json) {
        const result = new SourceLocale();

        result.id = json.id;

        return result;
    }
}