import uuidv4 from "uuid/v4";

export default class Screenshot {
    constructor() {
        this.name = null;
        this.locale = null;
        this.src = null;
        this.branch = null;
        this.textUnits = [];
    }

    static branchStatisticsContentToScreenshot(branchStatisticsContent, imageUrl, textUnitChecked) {
        let result = new Screenshot();

        result.name = uuidv4();
        result.locale = branchStatisticsContent.branch.repository.sourceLocale;
        result.src = imageUrl;
        result.branch = {id: branchStatisticsContent.branch.id};

        result.textUnits = [];
        for(let i = 0; i < textUnitChecked.length; i++) {
            if(textUnitChecked[i]) {
                let tmTextUnit = new TmTextUnit(branchStatisticsContent.branchTextUnitStatistics[i].tmTextUnit.id);
                result.textUnits.push(new TextUnit(tmTextUnit));
            }
        }

        return result;
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