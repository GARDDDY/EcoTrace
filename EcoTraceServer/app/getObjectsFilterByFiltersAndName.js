// const express = require('express');
// const admin = /*unused!*/require('firebase-admin');

// const router = express.Router();

// var dbObjName;
// var filterName = null;

// const logError = (error) => console.error("Error:", error);

// function isOkName(objClass) {
//     return true;//filterName == null || objClass.username ? objClass.username.includes(filterName) : true
// }

// async function getFirst (filter) {
//     try {
//         const ref = admin.database().ref(dbObjName)
//             .orderByChild("filters").equalTo(filter).limitToFirst(1);
//         const snapshot = await ref.once('value');
//         const value = snapshot.val();
//         return Object.keys(value)[0];
//     } catch (error) {
//         // logError(error);
//         return null;
//     }
// };

// async function getLast (filter) {
//     try {
//         const ref = admin.database().ref(dbObjName)
//             .orderByChild("filters").equalTo(filter).limitToLast(1);
//         const snapshot = await ref.once('value');
//         return Object.keys(snapshot.val())[0];
//     } catch (error) {
//         // logError(error);
//         return null;
//     }
// };
// async function getFilterValue(key) {
//     try {
//         const ref = admin.database().ref(`${dbObjName}/${key}/filters`);
//         const snapshot = await ref.once('value');
//         return snapshot.exists() ? snapshot.val() : "";
//     } catch (error) {
//         // logError(error);
//         return "";
//     }
// };

// async function getNextKey (key, value, defaultFilter, filterByFilters) {
//     try {
//         console.log(`getting next key ${key} ${value} ${defaultFilter} ${filterByFilters}`)
//         const ref = admin.database().ref(dbObjName);
//         let query;

//         if (filterByFilters) {
//             query = ref.orderByChild("filters").startAfter(value, key).limitToFirst(1);
//         } else {
//             query = ref.orderByKey().startAfter(key).limitToFirst(1);
//         }

//         const snapshot = await query.once('value');
//         const nextValueFilter = Object.values(snapshot.val())[0].filters || "";

//         return (nextValueFilter.includes(defaultFilter) || !filterByFilters) ? Object.keys(snapshot.val())[0] : null;
//     } catch (error) {
//         logError(error);
//         return null;
//     }
// };


// function addOneFilter (filter, lastAdded = 0) {
//     console.log(`filter ${filter}`);
//     const filterParts = filter.includes(':') ? filter.split(':') : filter;
//     console.log(`filterParts ${filterParts}`);
//     const currentFilter = filterParts.includes(',') ? filterParts[1].split(',').map(Number) : [parseInt(filterParts)];
//     console.log(`current ${currentFilter}`);
//     let nextFilter = lastAdded + 1;

//     console.log(nextFilter);

//     if (nextFilter > 0 && currentFilter.length < 2) {
//         while (currentFilter.includes(nextFilter) && nextFilter <= 2) {
//             nextFilter++;
//         }
//         if (nextFilter > 2) {
//             return [filter, lastAdded];
//         }
//         currentFilter.push(nextFilter);
//     }
//     const resultFilter = `${currentFilter.length}:${currentFilter.sort((a, b) => a - b).join(",")}`;
//     return [resultFilter, nextFilter];
// };

// router.get("/getObjectFiltered", async (req, res) => {
//     const filters = req.query.filters || "";
//     var newObjectId = req.query.nei || null;
//     if (newObjectId == "null") newObjectId = null;
//     const objName = req.query.obj;
//     var name = req.query.name || null;
//     if (name == "null") name = null;
//     dbObjName = objName;
//     filterName = name;

//     const foundObjects = [];
//     const ref = admin.database().ref(objName);

//     let startAt = newObjectId;
//     let nextObj = null;
//     let lastValue = "";
//     let lastAdded = -1;

//     let lastLast = null;
//     let cycle = 0;
//     let filterByFilters = true;

//     while (foundObjects.length <= 3) {
//         if (filters != "" && lastLast == null || (lastLast != null && lastLast === nextObj)) {
//             const [filter, added] = addOneFilter(filters, lastAdded);
//             lastAdded = added;
//             startAt = newObjectId || await getFirst(filter);
//             lastLast = await getLast(filter);
//             cycle = 0;
//             if (startAt == null) continue;
//         }

//         const filterValue = startAt ? await getFilterValue(startAt) : null;

//         let query;
//         if (filterValue != null) {
//             query = lastLast != null && cycle !== 0
//                 ? ref.orderByChild("filters").startAfter(filterValue, nextObj).limitToFirst(1)
//                 : ref.orderByChild("filters").startAt(filterValue, startAt).limitToFirst(1);
//         } else {
//             filterByFilters = false;
//             query = ref.orderByKey();
//             if (newObjectId != null) query = query.startAt(newObjectId);
//             query = query.limitToFirst(3);
//         }

//         const snapshot = await query.once('value');
//         const data = snapshot.val();

//         if (!data) {
//             return res.json(foundObjects);
//         }

//         Object.keys(data).forEach(key => {
//             const objectClass = data[key];
//             if (cycle !== 0 && foundObjects.some(e => e.objectId === key)) {
//                 return res.json(foundObjects);
//             }

//             if (isOkName(objectClass)) {
//                 foundObjects.push({ objectId: key, filters: objectClass.filters.split(':')[1], name: objectClass.username }); //////!!!!
//             }

//             nextObj = key;
//             lastValue = objectClass.filters || "";
//         });

//         if (foundObjects.length >= 3) break;
//         cycle += 1;
//         if (cycle >= 2) break;
//     }

//     nextObj = nextObj ? await getNextKey(nextObj, lastValue, filters, filterByFilters) : null;
//     res.json([[nextObj, nextObj === null], foundObjects]);
// });

// module.exports = router;
