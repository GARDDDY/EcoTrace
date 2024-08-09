<?php

require __DIR__.'/vendor/autoload.php';
require __DIR__.'/dbMain.php';
use Kreait\Firebase\Factory;

$userId = $_GET["uid"] ?? '0';
$oAuth = $_GET["oauth"] ?? '0';

// if (($userId != '0' && $oAuth != '0') &&
// !(new FirebaseCheckAuth())->checkOAuth2($oAuth, $userId)) 
// {
//     echo json_encode(["error" => "Suspicios request detected!"]);
//     exit();
// }

$dbStatics = new FirebaseStatic();
$factory = (new Factory)
->withServiceAccount($dbStatics->dbServiceAccountPath)
->withDatabaseUri($dbStatics->dbDefaultUri);

$topUsers = $factory->createDatabase()->getReference("users")
->orderByChild("experience")->limitToLast(30)
->getSnapshot()->getValue();

$tops = array();

foreach($topUsers as $uid => $userData) {
    $tops[$uid] = json_decode(json_encode([
        "userId" => $uid,
        "experience" => $userData["experience"],
        "username" => $userData["username"]
    ]));
}

header('Content-Type: application/json; charset=utf-8');
echo json_encode($tops);

?>