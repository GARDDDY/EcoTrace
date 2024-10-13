// to sql, unused?

async function getRules(userId, rule) {
    let rulesArray = ['countrySeen', 'nameSeen', 'friendFrom', 'showGroups'];

    if (rule > rulesArray.length) {
        throw new Error('Invalid rule index');
    }

    try {
        const reference = admin.database().ref('users/' + userId + '/rules/' + rulesArray[rule]);
        const snapshot = await reference.once('value');
        const value = snapshot.val() || 2;
        return value;
    } catch (error) {
        return 2;
    }
}

module.exports = { getRules }