import TagsBlockEncoder from './TagsBlockEncoder';

class IctMetadataBuilder {

    getTranslationWithMetadata(repositoryName, assetName, textUnitName, localeName, stack, translation, translationType) {
        var textUnitMetadata = this.getTextUnitMetadata(repositoryName, assetName, textUnitName, localeName, stack, translationType);
        var textUnitMetadataAsTagsBlock = TagsBlockEncoder.unicodeToTagsBlock(textUnitMetadata);
        return this.getStartDelimiter() + textUnitMetadataAsTagsBlock + this.getMiddleDelimiter() + translation + this.getEndDelimiter();
    }

    getTextUnitMetadata(repositoryName, assetName, textUnitName, localeName, stack, translationType) {
        return repositoryName + this.getInnerDelimiter()
                + (assetName ? assetName : "") + this.getInnerDelimiter()
                + textUnitName + this.getInnerDelimiter()
                + localeName + this.getInnerDelimiter()
                + stack + this.getInnerDelimiter()
                + translationType;
    }

    getStartDelimiter() {
        return "\u{E0022}";
    }

    getMiddleDelimiter() {
        return "\u{E0023}";
    }

    getEndDelimiter() {
        return "\u{E0024}";
    }

    getInnerDelimiter() {
        return "\u{0013}";
    }
}

export default new IctMetadataBuilder();