const zlib = require('zlib');

function buildCrcTable() {
    const table = new Uint32Array(256);
    for (let n = 0; n < 256; n++) {
        let c = n;
        for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
        table[n] = c >>> 0;
    }
    return table;
}
const CRC_TABLE = buildCrcTable();

function crc32(buf) {
    let crc = 0xFFFFFFFF;
    for (let i = 0; i < buf.length; i++) crc = CRC_TABLE[(crc ^ buf[i]) & 0xFF] ^ (crc >>> 8);
    return (crc ^ 0xFFFFFFFF) >>> 0;
}

function pngChunk(type, data) {
    const typeBuf = Buffer.from(type, 'ascii');
    const lenBuf = Buffer.alloc(4);
    lenBuf.writeUInt32BE(data.length, 0);
    const crcBuf = Buffer.alloc(4);
    crcBuf.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0);
    return Buffer.concat([lenBuf, typeBuf, data, crcBuf]);
}

function hslToRgb(h, s, l) {
    h = ((h % 360) + 360) % 360;
    const c = (1 - Math.abs(2 * l - 1)) * s;
    const x = c * (1 - Math.abs((h / 60) % 2 - 1));
    const m = l - c / 2;
    let r = 0, g = 0, b = 0;
    if (h < 60) [r, g, b] = [c, x, 0];
    else if (h < 120) [r, g, b] = [x, c, 0];
    else if (h < 180) [r, g, b] = [0, c, x];
    else if (h < 240) [r, g, b] = [0, x, c];
    else if (h < 300) [r, g, b] = [x, 0, c];
    else [r, g, b] = [c, 0, x];
    return [Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255)];
}

function hashString(str) {
    let hash = 2166136261;
    for (let i = 0; i < str.length; i++) {
        hash ^= str.charCodeAt(i);
        hash = Math.imul(hash, 16777619);
    }
    return hash >>> 0;
}

const SIZE = 512;

function generateGroupGradientPng(seedText) {
    const hash = hashString(seedText);
    const hue = hash % 360;
    const hueShift = 30 + (Math.floor(hash / 360) % 60);
    const edgeColor = hslToRgb(hue, 0.55, 0.42);
    const centerColor = hslToRgb((hue + hueShift) % 360, 0.5, 0.62);

    const cx = SIZE / 2;
    const cy = SIZE / 2;
    const maxDist = Math.sqrt(cx * cx + cy * cy);

    const raw = Buffer.alloc(SIZE * (1 + SIZE * 3));
    let offset = 0;
    for (let y = 0; y < SIZE; y++) {
        raw[offset++] = 0;
        for (let x = 0; x < SIZE; x++) {
            const dx = x - cx;
            const dy = y - cy;
            const t = Math.min(1, Math.sqrt(dx * dx + dy * dy) / maxDist);
            raw[offset++] = Math.round(centerColor[0] + (edgeColor[0] - centerColor[0]) * t);
            raw[offset++] = Math.round(centerColor[1] + (edgeColor[1] - centerColor[1]) * t);
            raw[offset++] = Math.round(centerColor[2] + (edgeColor[2] - centerColor[2]) * t);
        }
    }

    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(SIZE, 0);
    ihdr.writeUInt32BE(SIZE, 4);
    ihdr[8] = 8;
    ihdr[9] = 2;
    ihdr[10] = 0;
    ihdr[11] = 0;
    ihdr[12] = 0;

    const idatData = zlib.deflateSync(raw);
    const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

    return Buffer.concat([
        signature,
        pngChunk('IHDR', ihdr),
        pngChunk('IDAT', idatData),
        pngChunk('IEND', Buffer.alloc(0)),
    ]);
}

module.exports = { generateGroupGradientPng };
