<?php
// This file sends FCM notifications using Firebase Admin SDK
// You need to install: composer require google/apiclient

require_once 'connection.php';

function sendFCMNotification($fcm_token, $title, $body, $data = []) {
    // Path to your Firebase service account key JSON file
    $serviceAccountPath = __DIR__ . '/firebase-adminsdk.json';

    if (!file_exists($serviceAccountPath)) {
        error_log("Firebase service account file not found");
        return false;
    }

    // Get access token
    $accessToken = getAccessToken($serviceAccountPath);

    if (!$accessToken) {
        error_log("Failed to get Firebase access token");
        return false;
    }

    // Get project ID from service account
    $serviceAccount = json_decode(file_get_contents($serviceAccountPath), true);
    $projectId = $serviceAccount['project_id'];

    // FCM API endpoint
    $url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send";

    // Notification payload
    $notification = [
        'message' => [
            'token' => $fcm_token,
            'notification' => [
                'title' => $title,
                'body' => $body
            ],
            'data' => $data,
            'android' => [
                'priority' => 'high',
                'notification' => [
                    'sound' => 'default',
                    'click_action' => 'FLUTTER_NOTIFICATION_CLICK'
                ]
            ]
        ]
    ];

    $headers = [
        'Authorization: Bearer ' . $accessToken,
        'Content-Type: application/json'
    ];

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($notification));

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($httpCode == 200) {
        return true;
    } else {
        error_log("FCM send failed: $response");
        return false;
    }
}

function getAccessToken($serviceAccountPath) {
    $serviceAccount = json_decode(file_get_contents($serviceAccountPath), true);

    $now = time();
    $payload = [
        'iss' => $serviceAccount['client_email'],
        'sub' => $serviceAccount['client_email'],
        'aud' => 'https://oauth2.googleapis.com/token',
        'iat' => $now,
        'exp' => $now + 3600,
        'scope' => 'https://www.googleapis.com/auth/firebase.messaging'
    ];

    // Create JWT
    $header = json_encode(['alg' => 'RS256', 'typ' => 'JWT']);
    $payload = json_encode($payload);

    $base64UrlHeader = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($header));
    $base64UrlPayload = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($payload));

    $signature = '';
    openssl_sign(
        $base64UrlHeader . "." . $base64UrlPayload,
        $signature,
        $serviceAccount['private_key'],
        'SHA256'
    );

    $base64UrlSignature = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($signature));
    $jwt = $base64UrlHeader . "." . $base64UrlPayload . "." . $base64UrlSignature;

    // Exchange JWT for access token
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, 'https://oauth2.googleapis.com/token');
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query([
        'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion' => $jwt
    ]));

    $response = curl_exec($ch);
    curl_close($ch);

    $data = json_decode($response, true);
    return isset($data['access_token']) ? $data['access_token'] : null;
}

// Function to notify followers about new recipe
function notifyFollowersAboutNewRecipe($user_id, $recipe_id, $recipe_title) {
    global $conn;

    // Get all followers of this user
    $sql = "SELECT u.fcm_token, u.username
            FROM Followers f
            INNER JOIN Users u ON f.follower_id = u.user_id
            WHERE f.following_id = ? AND u.fcm_token IS NOT NULL";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();

    // Get the recipe author's name
    $authorSql = "SELECT username FROM Users WHERE user_id = ?";
    $authorStmt = $conn->prepare($authorSql);
    $authorStmt->bind_param("i", $user_id);
    $authorStmt->execute();
    $authorResult = $authorStmt->get_result();
    $author = $authorResult->fetch_assoc();
    $authorName = $author['username'] ?? 'Someone';

    while ($row = $result->fetch_assoc()) {
        $fcm_token = $row['fcm_token'];
        $title = "New Recipe from $authorName";
        $body = "Check out the new recipe: $recipe_title";
        $data = [
            'type' => 'new_recipe',
            'recipe_id' => (string)$recipe_id,
            'title' => $title,
            'body' => $body
        ];

        sendFCMNotification($fcm_token, $title, $body, $data);
    }

    $stmt->close();
    $authorStmt->close();
}

// Function to notify user about new follower
function notifyNewFollower($followed_user_id, $follower_username) {
    global $conn;

    error_log("Attempting to notify user $followed_user_id about new follower $follower_username");

    // Get the followed user's FCM token
    $sql = "SELECT fcm_token FROM Users WHERE user_id = ? AND fcm_token IS NOT NULL";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $followed_user_id);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($row = $result->fetch_assoc()) {
        $fcm_token = $row['fcm_token'];
        error_log("Found FCM token for user $followed_user_id, sending notification...");

        $title = "New Follower";
        $body = "$follower_username started following you!";
        $data = [
            'type' => 'new_follower',
            'title' => $title,
            'body' => $body
        ];

        $success = sendFCMNotification($fcm_token, $title, $body, $data);
        if ($success) {
            error_log("FCM notification sent successfully to user $followed_user_id");
        } else {
            error_log("Failed to send FCM notification to user $followed_user_id");
        }
    } else {
        error_log("User $followed_user_id does not have an FCM token registered");
    }

    $stmt->close();
}
?>

