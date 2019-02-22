export default class Screenshot {
    constructor() {
        this.name = null;
        this.locale = null;
        this.src = null;
        this.branch = null;
        this.textUnits = [];
    }


}

export class TextUnit {
    constructor(tmTextUnit) {
        this.tmTextUnit = tmTextUnit;
    }
}

export class TmTextUnit {
    constructor(id) {
        this.id = id;
    }
}