<?php

require __DIR__.'/vendor/autoload.php';
require __DIR__.'/dbMain.php';
use Kreait\Firebase\Factory;

$dbStatics = new FirebaseConfig();
$dbUserInter = new UserMain($dbStatics);
$factory = (new Factory)
->withServiceAccount($dbStatics->dbServiceAccountPath)
->withDatabaseUri($dbStatics->dbDefaultUri);

$userId = $_GET['uid'] ?? '-N4a2b3c4d5e6f7g8h9iM';
$block = $_GET['block'] ?? null;
if ($block == "null") {
    $block = null;
}
$oAuth = $_GET['oauth'] ?? '0';
$requestFrom = $_GET['cid'] ?? '0';

header('Content-Type: application/json; charset=utf-8');

// if (!(new FirebaseCheckAuth())->checkOAuth2($oAuth, $requestFrom)) {
//     echo json_encode(["error" => "You are not signed in! Not allowed ev"]);
//     exit();
// }

function get($factory, $block, $userId, $requestFrom, $dbUserInter, &$dataGlobal, &$repeated) {
    $isOwner = $requestFrom === $userId && $userId !== '0';
    $reference = $factory
        ->createDatabase()
        ->getReference('users/' . $userId . '/events')
        ->orderByKey();

    if ($block !== null) {
        $reference = $reference->startAfter($block);
    }

    $data = $reference
        ->limitToFirst(2)
        ->getSnapshot()->getValue() ?? [];
    
    $lastGid = null;
    $groupsRule = $dbUserInter->getUserRule($userId, 4);
    $isFriend = $dbUserInter->areUsersFriends($userId, $requestFrom);

    $filteredData = array_filter($data, function($gData) use ($isOwner, $groupsRule, $isFriend) {
        return $isOwner || 
               ($groupsRule == 0 && $gData) || 
               ($groupsRule == 1 && $isFriend && $gData);
    }, ARRAY_FILTER_USE_BOTH);

    $lastGid = array_key_last($filteredData);

    if ($repeated || count($filteredData) >= 2) {
        $dataGlobal = array_merge($dataGlobal, $filteredData);
        echo json_encode(empty($dataGlobal) ? null : $dataGlobal);
    } else {
        $dataGlobal = array_merge($dataGlobal, $filteredData);
        $repeated = true;
        $block = $lastGid;
        get($factory, $block, $userId, $requestFrom, $dbUserInter, $dataGlobal, $repeated);
    }
}

$dataGlobal = [];
$repeated = false;
get($factory, $block, $userId, $requestFrom, $dbUserInter, $dataGlobal, $repeated);

?>