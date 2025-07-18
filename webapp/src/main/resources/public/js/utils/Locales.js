import Cldr from "cldrjs";
import _ from "lodash";

import likelySubtags from 'cldr-data/supplemental/likelySubtags.json';

import beLanguages from 'cldr-data/main/be/languages.json';
import beTerritories from 'cldr-data/main/be/territories.json';
import beScripts from 'cldr-data/main/be/scripts.json';

import enLanguages from 'cldr-data/main/en/languages.json';
import enTerritories from 'cldr-data/main/en/territories.json';
import enScripts from 'cldr-data/main/en/scripts.json';

import frLanguages from 'cldr-data/main/fr/languages.json';
import frTerritories from 'cldr-data/main/fr/territories.json';
import frScripts from 'cldr-data/main/fr/scripts.json';

import koLanguages from 'cldr-data/main/ko/languages.json';
import koTerritories from 'cldr-data/main/ko/territories.json';
import koScripts from 'cldr-data/main/ko/scripts.json';

import ruLanguages from 'cldr-data/main/ru/languages.json';
import ruTerritories from 'cldr-data/main/ru/territories.json';
import ruScripts from 'cldr-data/main/ru/scripts.json';

import deLanguages from 'cldr-data/main/de/languages.json';
import deTerritories from 'cldr-data/main/de/territories.json';
import deScripts from 'cldr-data/main/de/scripts.json';

import esLanguages from 'cldr-data/main/es/languages.json';
import esTerritories from 'cldr-data/main/es/territories.json';
import esScripts from 'cldr-data/main/es/scripts.json';

import itLanguages from 'cldr-data/main/it/languages.json';
import itTerritories from 'cldr-data/main/it/territories.json';
import itScripts from 'cldr-data/main/it/scripts.json';

import jaLanguages from 'cldr-data/main/ja/languages.json';
import jaTerritories from 'cldr-data/main/ja/territories.json';
import jaScripts from 'cldr-data/main/ja/scripts.json';

import ptLanguages from 'cldr-data/main/pt/languages.json';
import ptTerritories from 'cldr-data/main/pt/territories.json';
import ptScripts from 'cldr-data/main/pt/scripts.json';

import zhHansLanguages from 'cldr-data/main/zh-Hans/languages.json';
import zhHansTerritories from 'cldr-data/main/zh-Hans/territories.json';
import zhHansScripts from 'cldr-data/main/zh-Hans/scripts.json';

import zhHantLanguages from 'cldr-data/main/zh-Hant/languages.json';
import zhHantTerritories from 'cldr-data/main/zh-Hant/territories.json';
import zhHantScripts from 'cldr-data/main/zh-Hant/scripts.json';

Cldr.load(
  likelySubtags,
  beLanguages, beTerritories, beScripts,
  enLanguages, enTerritories, enScripts,
  frLanguages, frTerritories, frScripts,
  koLanguages, koTerritories, koScripts,
  ruLanguages, ruTerritories, ruScripts,
  deLanguages, deTerritories, deScripts,
  esLanguages, esTerritories, esScripts,
  itLanguages, itTerritories, itScripts,
  jaLanguages, jaTerritories, jaScripts,
  ptLanguages, ptTerritories, ptScripts,
  zhHansLanguages, zhHansTerritories, zhHansScripts,
  zhHantLanguages, zhHantTerritories, zhHantScripts
);

class Locales {

    constructor() {
        //TODO don't use global var for LOCALE?
        this.currentLocale = APP_CONFIG.locale;

        this.cldr = new Cldr(this.currentLocale);
    }

    /**
     * Gets the list of supported locales in the app.
     *
     * Must add CLDR data accordingly, see above.
     *
     * @returns {string[]}
     */
    getSupportedLocales() {
        return [
            'be',
            'de',
            'en',
            'es',
            'fr',
            'it',
            'ja',
            'ko',
            'pt',
            'ru',
            'zh-Hans',
            'zh-Hant'
        ];
    }

    /**
     * Gets a list of language that are RTL
     * @returns {string[]}
     */
    getRightToLeftLanguages() {

        return [
            'ar',
            'arc',
            'bqi',
            'ckb',
            'dv',
            'fa',
            'glk',
            'he',
            'lrc',
            'mzn',
            'pnb',
            'ps',
            'sd',
            'ug',
            'ur',
            'yi'
        ];
    }


    /**
     * Gets the current locale of the app as a bcp47 tag
     *
     * @returns {string} bcp47 tag of the app locale
     */
    getCurrentLocale() {
        return this.currentLocale;
    }


    /**
     * Gets the display name of the current locale
     *
     * @returns {string} the display name of the current locale
     */
    getCurrentLocaleDisplayName() {
        return this.getDisplayName(this.getCurrentLocale());
    }

    /**
     * Gets the locale display name in current language, format: language (territory)
     *
     * @param bcp47Tag the bcp47 tag
     * @returns {string} the locale display name
     */
    getDisplayName(bcp47Tag) {
        const localeSubTags = this.getLocaleSubTags(bcp47Tag);

        const languageDisplay = this.cldr.main("localeDisplayNames/languages/" + localeSubTags.language);
        const scriptDisplay = this.cldr.main("localeDisplayNames/scripts/" + localeSubTags.script);
        const territoryDisplay = this.cldr.main("localeDisplayNames/territories/" + localeSubTags.territory);

        let localeDisplayName;

        if (languageDisplay === undefined) {
            localeDisplayName = bcp47Tag;
        } else {
            localeDisplayName = languageDisplay;

            // eslint-disable-next-line eqeqeq
            if (localeSubTags.script != null) {
                localeDisplayName = `${localeDisplayName} - ${scriptDisplay}`;
            }

            // eslint-disable-next-line eqeqeq
            if (localeSubTags.territory != null) {
                localeDisplayName = `${localeDisplayName} (${territoryDisplay})`;
            }
        }

        return localeDisplayName;
    }

    /**
     * Gets the native display name of a language.
     * Eg. getNativeDispalyName('be') -> беларуская
     *
     * @param language language
     * @returns {string} the native display name
     */
    getNativeDispalyName(language) {
        const targetCldr = new Cldr(language);

        const languageDisplay = targetCldr.main("localeDisplayNames/languages/" + language);

        return languageDisplay;
    }

    /**
     * Gets the direction (ltr or rtl) of a locale
     *
     * @param bcp47Tag bcp47 tag of the locale
     * @returns {string} the direction (ltr or rtl)
     */
    getLanguageDirection(bcp47Tag) {
        const language = this.getLocaleSubTags(bcp47Tag).language;

        let dir = "ltr";

        if (_.includes(this.getRightToLeftLanguages(), language)) {
            dir = "rtl";
        }

        return dir;
    }

    /**
     * Returns sorted objects by the locale display name
     *
     * @param {Object[]} objects array of object that contains locale
     * @param {Function} getBcp47Tag function to retrieve bcp47Tag from the object
     * @returns {Object[]} sorted objects
     */
    sortByDisplayName(objects, getBcp47Tag) {
        return objects
            .map(object => {
                const bcp47Tag = getBcp47Tag(object);
                object.localeDisplayName = this.getDisplayName(bcp47Tag);
                return object;
            })
            .sort((a, b) => a.localeDisplayName.localeCompare(b.localeDisplayName));
    }

    getLocaleSubTags(locale) {
        const subtags = this.cldrJsSubtags(locale);
        return {
            language: subtags[0],
            script: subtags[1] === "Zzzz" ? null : subtags[1],
            territory: subtags[2] === "ZZ" ? null : subtags[2],
            variant: subtags[3],
            unicodeLocaleExtensions: subtags[4]
        };
    }

    /**
     * This is coming from Cldr.js but it doesn't seem to be exposed so just copying over.
     *
     * subtags( locale )
     *
     * @locale [String]
     */
    cldrJsSubtags(locale) {
        var aux,
            unicodeLanguageId,
            subtags = [];

        locale = locale.replace(/_/, "-");

        // Unicode locale extensions.
        aux = locale.split("-u-");
        if (aux[1]) {
            aux[1] = aux[1].split("-t-");
            locale = aux[0] + (aux[1][1] ? "-t-" + aux[1][1] : "");
            subtags[4 /* unicodeLocaleExtensions */] = aux[1][0];
        }

        // TODO normalize transformed extensions. Currently, skipped.
        // subtags[ x ] = locale.split( "-t-" )[ 1 ];
        unicodeLanguageId = locale.split("-t-")[0];

        // unicode_language_id = "root"
        //   | unicode_language_subtag
        //     (sep unicode_script_subtag)?
        //     (sep unicode_region_subtag)?
        //     (sep unicode_variant_subtag)* ;
        //
        // Although unicode_language_subtag = alpha{2,8}, I'm using alpha{2,3}. Because, there's no language on CLDR lengthier than 3.
        aux = unicodeLanguageId.match(
            /^(([a-z]{2,3})(-([A-Z][a-z]{3}))?(-([A-Z]{2}|[0-9]{3}))?)((-([a-zA-Z0-9]{5,8}|[0-9][a-zA-Z0-9]{3}))*)$|^(root)$/
        );
        if (aux === null) {
            return ["und", "Zzzz", "ZZ"];
        }
        subtags[0 /* language */] = aux[10] /* root */ || aux[2] || "und";
        subtags[1 /* script */] = aux[4] || "Zzzz";
        subtags[2 /* territory */] = aux[6] || "ZZ";
        if (aux[7] && aux[7].length) {
            subtags[3 /* variant */] = aux[7].slice(1) /* remove leading "-" */;
        }

        // 0: language
        // 1: script
        // 2: territory (aka region)
        // 3: variant
        // 4: unicodeLocaleExtensions
        return subtags;
    }

};

export default new Locales();
