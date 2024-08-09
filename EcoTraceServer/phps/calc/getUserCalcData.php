<?php

require __DIR__.'/vendor/autoload.php';
require __DIR__.'/dbMain.php';
use Kreait\Firebase\Factory;

$userId = $_GET["uid"] ?? '0';
$oAuth = $_GET["oauth"] ?? '0';

$calcType = $_GET["calcType"] ?? 0;

if (!(new FirebaseCheckAuth())->checkOAuth2($oAuth, $requestFrom)) {
    echo json_encode(["error" => "Suspicios request detected!"]);
    exit();
}

$dbStatics = new FirebaseStatic();
$factory = (new Factory)
->withServiceAccount($dbStatics->dbServiceAccountPath)
->withDatabaseUri($dbStatics->dbDefaultUri);

$userCalc = $factory->createDatabase()->getReference("users/".$userId."/ecoCalc/cType".$calcType)
->getSnapshot()->getValue();



?>