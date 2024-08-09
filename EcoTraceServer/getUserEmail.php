<?php

require __DIR__.'/vendor/autoload.php';
require __DIR__.'/dbMain.php';
use Kreait\Firebase\Factory;

$dbStatics = new FirebaseConfig();

// check if created!!!!!

$factory = (new Factory)
->withServiceAccount($dbStatics->dbServiceAccountPath)
->withDatabaseUri($dbStatics->dbDefaultUri);


$login = $_GET['lgn'] ?? '0';
$password = $_GET['pss'] ?? '0';

$db = $factory->createDatabase();

$userId = $db->getReference("indexes/".$login)->getSnapshot();

if (!$userId->exists()) {
    echo null;
    exit();
}

$user = $db->getReference("users/".$userId->getValue()."/private")->getSnapshot()->getValue();

if ($password == $user["password"]) {
    echo $user["email"];
} else echo null;

?>