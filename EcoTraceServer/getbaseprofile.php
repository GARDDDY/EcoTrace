<?php

require __DIR__.'/vendor/autoload.php';
require __DIR__.'/dbMain.php';
use Kreait\Firebase\Factory;

$dbStatics = new FirebaseConfig();
$factory = (new Factory)
->withServiceAccount($dbStatics->dbServiceAccountPath)
->withDatabaseUri($dbStatics->dbDefaultUri);

function getContent($userId) {
    global $factory;
    return $factory
        ->createDatabase()
        ->getReference('users/'.$userId)
        ->getSnapshot()
        ->getValue();
}

// function userExists($userId) {
//     global $factory;
//     return $factory
//         ->createDatabase()
//         ->getReference('users/'.$userId) != null;
// }

$userId = $_GET['uid'] ?? '0';
$requestFrom = $_GET['cid'] ?? '0';
$oAuth = $_GET['oauth'] ?? '0';
header('Content-Type: application/json; charset=utf-8');

// if (!userExists($userId) || !userExists($requestFrom)) {
//     echo json_encode(["error" => "Suspicios request detected!"]);
//     exit();
// }

// if (!(new FirebaseCheckAuth())->checkOAuth2($oAuth, $requestFrom)) {
//     echo json_encode(["error" => "You are not signed in! Not allowed bp"]);
//     exit();
// }

$rules = getContent($userId . '/rules');

$userData = getContent($userId);
$isFriend = isset($userData['friends'][$requestFrom]);
$isOwner = $userId == $requestFrom;

unset($userData['private']);
unset($userData['rules']);
unset($userData['friends']);
unset($userData['events']);
unset($userData['groups']);

function isNotAvailableToGet($collectionType) {
    global $rules, $isFriend, $isOwner;

    if ($isOwner) {
        return false;
    }

    if (isset($rules[$collectionType]) && $rules[$collectionType] == 0) {
        return false;
    }

    if (isset($rules[$collectionType]) && $rules[$collectionType] == 1) {
        return !$isFriend;
    }

    return true;
}

if (isNotAvailableToGet('countrySeen')) {
    unset($userData['country']);
}
if (isNotAvailableToGet('nameSeen')){
    unset($userData['fullname']);
}


echo json_encode($userData, JSON_UNESCAPED_UNICODE);

?>
