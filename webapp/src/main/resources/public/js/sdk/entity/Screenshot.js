export default class Screenshot {
    constructor() {
        this.name = null;
        this.locale = null;
        this.src = null;
        this.branch = null;
        this.textUnits = [];
    }


}

class TextUnit {
    constructor(tmTextUnit) {
        this.tmTextUnit = tmTextUnit;
    }
}

class TmTextUnit {
    constructor(id) {
        this.id = id;
    }
}