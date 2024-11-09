const path = require('path');
const fs = require('fs');

const ROOT = path.join(__dirname, 'uploads');

async function upload(bucket, filePath, buffer) {
    const dir = path.join(ROOT, bucket);
    await fs.promises.mkdir(dir, { recursive: true });
    await fs.promises.writeFile(path.join(dir, filePath), buffer);
    return { path: `${bucket}/${filePath}` };
}

function publicUrl(bucket, filePath) {
    return `/uploads/${bucket}/${filePath}`;
}

function localPath(bucket, filename) {
    return path.join(ROOT, bucket, filename);
}

module.exports = { upload, publicUrl, localPath };
