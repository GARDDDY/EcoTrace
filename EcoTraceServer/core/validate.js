const { HttpError } = require('./route');

function validate(schema, data) {
    const result = schema.safeParse(data);
    if (!result.success) {
        throw new HttpError(400, 'validation error', result.error.flatten().fieldErrors);
    }
    return result.data;
}

module.exports = { validate };
