const crypto = require('crypto');
const { upload: storageUpload } = require('../storage');

async function uploadImage(file, bucket) {
    if (!file) return null;
    const key = `${Date.now()}_${crypto.randomBytes(4).toString('hex')}`;
    await storageUpload(bucket, `${key}.png`, file.buffer, file.mimetype);
    return key;
}

async function uploadImages(files, bucket) {
    if (!files || files.length === 0) return [];
    return Promise.all(files.map((f) => uploadImage(f, bucket)));
}

module.exports = { uploadImage, uploadImages };
