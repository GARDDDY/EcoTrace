const { upload: storageUpload } = require('../storage');

async function uploadImage(file, bucket) {
    if (!file) return null;
    const key = Date.now().toString();
    await storageUpload(bucket, `${key}.png`, file.buffer, file.mimetype);
    return key;
}

module.exports = { uploadImage };
