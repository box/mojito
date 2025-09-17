const CRC_TABLE = (() => {
    const table = new Uint32Array(256);
    for (let i = 0; i < 256; i++) {
        let c = i;
        for (let k = 0; k < 8; k++) {
            if ((c & 1) !== 0) {
                c = 0xedb88320 ^ (c >>> 1);
            } else {
                c = c >>> 1;
            }
        }
        table[i] = c >>> 0;
    }
    return table;
})();

function crc32(data) {
    let crc = 0 ^ (-1);
    for (let i = 0; i < data.length; i++) {
        crc = CRC_TABLE[(crc ^ data[i]) & 0xff] ^ (crc >>> 8);
    }
    return (crc ^ (-1)) >>> 0;
}

export function buildZipFile(files) {
    const textEncoder = new TextEncoder();
    const chunks = [];
    const centralChunks = [];
    let offset = 0;

    files.forEach(file => {
        const nameBytes = textEncoder.encode(file.name);
        const content = file.content instanceof Uint8Array ? file.content : new Uint8Array();
        const crc = crc32(content);

        const localHeader = new Uint8Array(30 + nameBytes.length);
        const localView = new DataView(localHeader.buffer);
        localView.setUint32(0, 0x04034b50, true); // local file header signature
        localView.setUint16(4, 20, true); // version needed to extract
        localView.setUint16(6, 0, true); // general purpose bit flag
        localView.setUint16(8, 0, true); // compression method (store)
        localView.setUint16(10, 0, true); // last mod file time
        localView.setUint16(12, 0, true); // last mod file date
        localView.setUint32(14, crc, true); // crc-32
        localView.setUint32(18, content.length, true); // compressed size
        localView.setUint32(22, content.length, true); // uncompressed size
        localView.setUint16(26, nameBytes.length, true); // file name length
        localView.setUint16(28, 0, true); // extra field length
        localHeader.set(nameBytes, 30);

        chunks.push(localHeader, content);

        const centralHeader = new Uint8Array(46 + nameBytes.length);
        const centralView = new DataView(centralHeader.buffer);
        centralView.setUint32(0, 0x02014b50, true); // central file header signature
        centralView.setUint16(4, 20, true); // version made by
        centralView.setUint16(6, 20, true); // version needed to extract
        centralView.setUint16(8, 0, true); // general purpose bit flag
        centralView.setUint16(10, 0, true); // compression method
        centralView.setUint16(12, 0, true); // last mod time
        centralView.setUint16(14, 0, true); // last mod date
        centralView.setUint32(16, crc, true); // crc-32
        centralView.setUint32(20, content.length, true); // compressed size
        centralView.setUint32(24, content.length, true); // uncompressed size
        centralView.setUint16(28, nameBytes.length, true); // file name length
        centralView.setUint16(30, 0, true); // extra field length
        centralView.setUint16(32, 0, true); // file comment length
        centralView.setUint16(34, 0, true); // disk number start
        centralView.setUint16(36, 0, true); // internal file attributes
        centralView.setUint32(38, 0, true); // external file attributes
        centralView.setUint32(42, offset, true); // relative offset of local header
        centralHeader.set(nameBytes, 46);

        centralChunks.push(centralHeader);
        offset += localHeader.length + content.length;
    });

    const centralDirectorySize = centralChunks.reduce((sum, chunk) => sum + chunk.length, 0);
    const endOfCentralDirectory = new Uint8Array(22);
    const endView = new DataView(endOfCentralDirectory.buffer);
    endView.setUint32(0, 0x06054b50, true); // end of central dir signature
    endView.setUint16(4, 0, true); // number of this disk
    endView.setUint16(6, 0, true); // disk where central directory starts
    endView.setUint16(8, files.length, true); // number of central directory records on this disk
    endView.setUint16(10, files.length, true); // total number of central directory records
    endView.setUint32(12, centralDirectorySize, true); // size of central directory
    endView.setUint32(16, offset, true); // offset of start of central directory
    endView.setUint16(20, 0, true); // comment length

    const totalSize = offset + centralDirectorySize + endOfCentralDirectory.length;
    const zipData = new Uint8Array(totalSize);
    let position = 0;

    chunks.forEach(chunk => {
        zipData.set(chunk, position);
        position += chunk.length;
    });

    centralChunks.forEach(chunk => {
        zipData.set(chunk, position);
        position += chunk.length;
    });

    zipData.set(endOfCentralDirectory, position);

    return zipData;
}
