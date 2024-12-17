class TagsBlockEncoder {

    unicodeToTagsBlock(id) {
        var base64 = this.b64EncodeUnicode(id);
        return this.asciiToTagsBlock(base64);
    }

    asciiToTagsBlock(string) {
        var res = '';

        [...string].forEach(c => {
            if (c.charCodeAt(0) >= 32 && c.charCodeAt(0) <= 126) {
                res += String.fromCharCode(56128, 56352 - 32 + c.charCodeAt(0));
            } else {
                throw "Unsupported character to encode in Tags block.";
            }
        });

        return res;
    }

    b64EncodeUnicode(str) {
        // from MDN: https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64/Base64_encoding_and_decoding
        // first we use encodeURIComponent to get percent-encoded UTF-8,
        // then we convert the percent encodings into raw bytes which
        // can be fed into btoa.
        return btoa(encodeURIComponent(str).replace(/%([0-9A-F]{2})/g,
                function toSolidBytes(match, p1) {
                    return String.fromCharCode('0x' + p1);
                }));
    }
}

export default new TagsBlockEncoder();