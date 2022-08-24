import TagsBlockDecoder from './TagsBlockDecoder';
import IctMetadataBuilder from './IctMetadataBuilder';

class IctMetadataExtractor {
    
    getTextUnits(string) {
        var textUnits = [];
        var startIdxs = [];
        var middleIdxs = [];

        for (var i = 0; i < string.length; i++) {
            var c = string.codePointAt(i);

            switch (c) {
                case IctMetadataBuilder.getStartDelimiter().codePointAt(0):
                    startIdxs.push(i);
                    break;
                case IctMetadataBuilder.getMiddleDelimiter().codePointAt(0):
                    middleIdxs.push(i);
                    break;
                case IctMetadataBuilder.getEndDelimiter().codePointAt(0):
                    var startIdx = startIdxs.pop();
                    var midIdx = middleIdxs.pop();
                    var endIdx = i;

                    var textUnit = {
                        'textUnitVariant': string.substring(midIdx + 2, endIdx)
                    };

                    var textUnitMetadata = string.substring(startIdx + 2, midIdx);

                    try {
                        var decodedTextUnitMedata = TagsBlockDecoder.tagsBlockToUnicode(textUnitMetadata);
                        var splitTextUnitMetadata = decodedTextUnitMedata.split(IctMetadataBuilder.getInnerDelimiter());
                        textUnit['repositoryName'] = splitTextUnitMetadata[0];
                        textUnit['assetName'] = splitTextUnitMetadata[1];
                        textUnit['textUnitName'] = splitTextUnitMetadata[2];
                        textUnit['locale'] = splitTextUnitMetadata[3];
                        textUnit['stack'] = splitTextUnitMetadata[4];
                        // For backwards compatibility check array length, if less than 5 set isTranslated to true
                        textUnit['isTranslated'] = splitTextUnitMetadata.length > 5 ? splitTextUnitMetadata[5] : 'true';

                    } catch (e) {
                        console.log("Can't extract ict metadata from string: ", string, e);
                    }
                    
                    textUnits.push(textUnit);
                    break;
            }
        }

        return textUnits;
    }

    hasMetadata(string) {
        return string.indexOf(IctMetadataBuilder.getStartDelimiter()) !== -1;
    }

    hasPartialMetadata(string) {
        return this.hasMetadata(string) && string.indexOf(IctMetadataBuilder.getEndDelimiter()) === -1;
    }

}

export default new IctMetadataExtractor();