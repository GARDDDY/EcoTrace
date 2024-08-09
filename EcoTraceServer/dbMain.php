<?php

require __DIR__.'/vendor/autoload.php';
use Kreait\Firebase\Factory;
use Kreait\Firebase\ServiceAccount;
use Kreait\Firebase\Auth;

use Kreait\Firebase\Auth\InvalidIdToken;

class FirebaseConfig {
    public $dbDefaultUri = "***.firebaseio.com"; // дефолтный путь к бд
    public $dbServiceAccountPath = '***-firebase-adminsdk-***.json'; // данные от сервис аккаунта

    public $MAX_GROUPS = 30;
}

class UserMain {
    private $database;
    private $firebaseConfig;

    public function __construct(FirebaseConfig $firebaseConfig) {
        $this->firebaseConfig = $firebaseConfig;
        $this->database = (new Factory)
            ->withServiceAccount($firebaseConfig->dbServiceAccountPath)
            ->withDatabaseUri($firebaseConfig->dbDefaultUri)
            ->createDatabase();
    }

    public function getUserRule($userId, $rule) {
        $rulesArray = ['countrySeen', 'nameSeen', 'friendFrom', 'showGroups', 'showEvents'];

        if (!isset($rulesArray[$rule])) {
            throw new InvalidArgumentException('Invalid rule index');
        }

        try {
            $reference = $this->database->getReference("users/$userId/rules/{$rulesArray[$rule]}");
            $value = $reference->getSnapshot()->getValue();
            return $value !== null ? $value : 2;
        } catch (Exception $e) {
            return 2;
        }
    }

    public function areUsersFriends($user1, $user2) {
        try {
            $reference = $this->database->getReference("users/$user1/friends/$user2");
            $value = $reference->getSnapshot()->getValue();
            return $value === true;
        } catch (Exception $e) {
            return false;
        }
    }

    public function getUsernameOnly($userId) {
        try {
            $reference = $this->database->getReference("users/$userId/username");
            return $reference->getSnapshot()->getValue();
        } catch (Exception $e) {
            return null;
        }
    }
}

class FirebaseCheckAuth {

    public function checkOAuth2($token, $userId) {
        if (empty($token) || empty($userId)) {
            throw new InvalidArgumentException('Bad data');
        }

        $logFile = 'checkOAuth2.log';
        $logMessage = '';

        try {
            $factory = (new Factory)
                ->withServiceAccount(FirebaseConfig::$dbServiceAccountPath)
                ->withDatabaseUri(FirebaseConfig::$dbDefaultUri);

            $auth = $factory->createAuth();

            try {
                $verifiedIdToken = $auth->verifyIdToken($token);
                $uid = $verifiedIdToken->claims()->get('sub');

                $isValid = $uid == $userId;
                $logMessage = date('Y-m-d H:i:s') . " Token Validity for User $userId - " . ($isValid ? 'Valid!' : 'Invalid: '.$uid);
                
                return $isValid;

            } catch (Exception $e) {
                $logMessage = date('Y-m-d H:i:s') . " Verify Error: " . $e->getMessage();
                return false;
            }
        } catch (Exception $e) {
            $logMessage = date('Y-m-d H:i:s') . " AuthException: " . $e->getMessage();
            return false;
        } finally {
            file_put_contents($logFile, $logMessage . PHP_EOL, FILE_APPEND);
        }
    }
    
}

?>