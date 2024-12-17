class TagsBlockDecoder {

    tagsBlockToUnicode(id) {
        var base64 = this.tagsBlockToAscii(id);
        return this.b64DecodeUnicode(base64);
    }

    removeTagsBlock(string) {
        return string.replace(/\udb40[\udc00-\udc7e]/g, '');
    }

    tagsBlockToAscii(string) {
        var res = '';

        [...string].forEach(c => {
            if (c.charCodeAt(0) === 56128 && c.charCodeAt(1) >= 56352 && c.charCodeAt(1) <= 56446) {
                res += String.fromCodePoint(32 + c.charCodeAt(1) - 56352);
            } else {
                throw "Unsupported character to decode in Tags block.";
            }
        });

        return res;
    }

    b64DecodeUnicode(str) {
        // from MDN: https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64/Base64_encoding_and_decoding
        // Going backwards: from bytestream, to percent-encoding, to original string.
        return decodeURIComponent(atob(str).split('').map(function (c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
    }
}

export default new TagsBlockDecoder();