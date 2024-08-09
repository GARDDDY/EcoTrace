<?php

require __DIR__.'/vendor/autoload.php';
require __DIR__.'/dbMain.php';
use Kreait\Firebase\Factory;

$dbStatics = new FirebaseConfig();
$dbUserInter = new UserMain($dbStatics);
$factory = (new Factory)
->withServiceAccount($dbStatics->dbServiceAccountPath)
->withDatabaseUri($dbStatics->dbDefaultUri);

$userId = $_GET['uid'] ?? '0';
$block = $_GET['block'] ?? null;
if ($block == "null") {
    $block = null;
}
$oAuth = $_GET['oauth'] ?? '0';
$requestFrom = $_GET['cid'] ?? '0';

header('Content-Type: application/json; charset=utf-8');

// if (!(new FirebaseCheckAuth())->checkOAuth2($oAuth, $requestFrom)) {
//     echo json_encode(["error" => "You are not signed in! Not allowed fr"]);
//     exit();
// }

function get($factory, $block, $userId, $requestFrom, $dbUserInter, &$dataGlobal, &$repeated) {
    $isOwner = $requestFrom === $userId && $userId !== '0';
    $reference = $factory
        ->createDatabase()
        ->getReference('users/' . $userId . '/friends')
        ->orderByKey();

    if ($block !== null) {
        $reference = $reference->startAfter($block);
    }

    $data = $reference
        ->limitToFirst(2)
        ->getSnapshot()->getValue() ?? [];
    
    $lastUid = null;
    $friendsRule = $dbUserInter->getUserRule($userId, 2);
    $isFriend = $dbUserInter->areUsersFriends($userId, $requestFrom);
    $filteredData = [];

    foreach ($data as $uid => $gData) {
        $shouldKeep = $isOwner || 
                      ($friendsRule == 0 && $gData['friend']) || 
                      ($friendsRule == 1 && $isFriend && $gData['friend']);

        if ($shouldKeep) {
            $filteredData[$uid] = $gData;
            $filteredData[$uid]["userId"] = $uid;
            $filteredData[$uid]["username"] = $dbUserInter->getUsernameOnly($uid);
        }

        $lastUid = $uid;
    }
    if ($repeated || count($filteredData) >= 2) {
        $dataGlobal = array_merge($dataGlobal, $filteredData);
        echo json_encode(empty($dataGlobal) ? null : $dataGlobal);
    } else {
        $dataGlobal = array_merge($dataGlobal, $filteredData);
        $repeated = true;
        $block = $lastUid;
        get($factory, $block, $userId, $requestFrom, $dbUserInter, $dataGlobal, $repeated);
    }
}

$dataGlobal = [];
$repeated = false;
get($factory, $block, $userId, $requestFrom, $dbUserInter, $dataGlobal, $repeated);

?>