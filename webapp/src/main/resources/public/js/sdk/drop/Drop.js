import User from "../entity/User";
import DropExporterConfig from "../drop/DropExporterConfig";
import PollableTask from "../entity/PollableTask";
import Repository from "../entity/Repository";
import TranslationKit from "../entity/TranslationKit";
import StatusFilter from "../entity/StatusFilter";

/**
 * Used to communicate with Drops WS
 */
export default class Drop {
    constructor() {

        /** @type {Number} */
        this.id = 0;

        /** @type {String} */
        this.name = "";

        /** @type {User} */
        this.createdByUser = null;

        /** @type {Date} */
        this.createdDate = null;

        /** @type {STATUS_TYPE} */
        this.status = "";

        /** @type {DropExporterConfig|null} */
        this.dropExporterConfig = null;

        /** @type {String} */
        this.dropExporterType = "";

        /** @type {Repository} */
        this.repository = null;

        /** @type {TranslationKit[]} */
        this.translationKits = null;

        /** @type {Date|null} */
        this.lastImportedDate = null;

        /** @type {Boolean|null} */
        this.canceled = false;

        /** @type {PollableTask} */
        this.importPollableTask = null;

        /** @type {PollableTask} */
        this.exportPollableTask = null;

        /** @type {Boolean|null} */
        this.exportFailed = false;

        /** @type {Boolean|null} */
        this.importFailed = false;
    }

    getId() {
        return this.id;
    }

    setId(id) {
        this.id = id;
    }

    getName() {
        return this.name;
    }

    setName(name) {
        this.name = name;
    }

    getCreatedByUser() {
        return this.createdByUser;
    }

    setCreatedByUser(createdBy) {
        this.createdByUser = createdBy;
    }

    getCreatedDate() {
        return this.createdDate;
    }

    setCreatedDate(createdDate) {
        this.createdDate = createdDate;
    }

    getStatus() {
        return this.status;
    }

    setStatus(status) {
        return this.status;
    }

    /**
     * @return {DropExporterConfig}
     */
    getDropExporterConfig() {
        return this.dropExporterConfig;
    }

    /**
     * @param {DropExporterConfig} dropExporterConfig
     */
    setDropExporterConfig(dropExporterConfig) {
        this.dropExporterConfig = dropExporterConfig;
    }

    /**
     * @return {string}
     */
    getdropExporterType() {
        return this.dropExporterType;
    }

    /**
     * @param {string} dropExporterType
     */
    setdropExporterType(dropExporterType) {
        this.dropExporterType = dropExporterType;
    }

    equals(Drop) {
        return this.getDropKey() === Drop.getDropKey();
    }

    /**
     * Should only call this when all the properties have been set
     * @return {STATUS_TYPE}
     */
    calculateStatus() {
        let status = Drop.STATUS_TYPE.IN_TRANSLATION;

        if (this.canceled) {
            status = Drop.STATUS_TYPE.CANCELED;
        } else if (this.exportFailed) {
            status = Drop.STATUS_TYPE.EXPORT_FAILED;
        } else if (this.importFailed) {
            status = Drop.STATUS_TYPE.IMPORT_FAILED;
        } else if (this.isBeingExported()) {
            status = Drop.STATUS_TYPE.SENDING;
        } else if (this.isBeingImported()) {
            status = Drop.STATUS_TYPE.IMPORTING;
        } else if (this.lastImportedDate !== null) {
            status = Drop.STATUS_TYPE.IMPORTED;
        } else if (this.translationKits.length > 0) {
            if (this.translationKits[0].type.equals(StatusFilter.Type.Translation)) {
                status = Drop.STATUS_TYPE.IN_TRANSLATION;
            } else if (this.translationKits[0].type.equals(StatusFilter.Type.Review)) {
                status = Drop.STATUS_TYPE.IN_REVIEW;
            }
        }

        return status;
    }

    /**
     * @return {boolean}
     */
    isBeingImported() {
        return (this.importPollableTask !== null && !this.importPollableTask.isAllFinished);
    }

    /**
     * @return {boolean}
     */
    isBeingExported() {
        return this.dropExporterConfig === null ||
            !this.exportPollableTask ||
            (this.exportPollableTask && !this.exportPollableTask.isAllFinished);
    }

    /**
     * Convert JSON Drop object
     *
     * @param {Object} json
     * @return {Drop}
     */
    static toDrop(json) {
        let result = new Drop();

        result.id = json.id;

        // @NOTE dropExporterConfig can be null.  This is because before Drops are done processing but after it was first
        // created, this property is null. 
        if (json.dropExporterConfig) {
            result.dropExporterConfig = DropExporterConfig.toDropExporterConfig(json.dropExporterConfig);
        }

        result.name = json.name;
        result.createdDate = new Date(json.createdDate);
        result.createdByUser = User.toUser(json.createdByUser);
        result.repository = Repository.toRepository(json.repository);


        if (json.lastImportedDate) {
            result.lastImportedDate = new Date(json.lastImportedDate);
        }

        if (json.canceled) {
            result.canceled = json.canceled;
        }

        if (json.exportFailed) {
            result.exportFailed = json.exportFailed;
        }

        if (json.importFailed) {
            result.importFailed = json.importFailed;
        }

        result.translationKits = TranslationKit.toTranslationKits(json.translationKits);

        result.importPollableTask = PollableTask.toPollableTask(json.importPollableTask);
        result.exportPollableTask = PollableTask.toPollableTask(json.exportPollableTask);

        // Only calculate when all the properties are set
        result.status = result.calculateStatus();
        return result;
    }

    /**
     *
     * @param {Object[]} jsons
     * @return {Drop[]}
     */
    static toDrops(jsons) {

        var results = [];

        for (let drop of jsons) {
            results.push(Drop.toDrop(drop));
        }

        return results;
    }
}

/** @typedef {STATUS_TYPE} Pseudo status ype for UI display purpose */
Drop.STATUS_TYPE = {
    SENDING: Symbol(),
    IN_TRANSLATION: Symbol(),
    IN_REVIEW: Symbol(),
    IMPORTED: Symbol(),
    IMPORTING: Symbol(),
    CANCELED: Symbol(),
    EXPORT_FAILED: Symbol(),
    IMPORT_FAILED: Symbol()
};
