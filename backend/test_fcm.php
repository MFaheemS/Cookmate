<?php
header('Content-Type: application/json');
require_once 'connection.php';

// Test file to check FCM setup

$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;

if ($user_id == 0) {
    echo json_encode(['status' => 'error', 'message' => 'No user_id provided']);
    exit();
}

// Check if fcm_token column exists
$columnCheck = $conn->query("SHOW COLUMNS FROM Users LIKE 'fcm_token'");
if ($columnCheck->num_rows == 0) {
    echo json_encode([
        'status' => 'error',
        'message' => 'fcm_token column does not exist in Users table',
        'fix' => 'Run: ALTER TABLE Users ADD COLUMN fcm_token VARCHAR(255) DEFAULT NULL;'
    ]);
    exit();
}

// Check if user has FCM token
$sql = "SELECT user_id, username, fcm_token FROM Users WHERE user_id = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("i", $user_id);
$stmt->execute();
$result = $stmt->get_result();

if ($row = $result->fetch_assoc()) {
    $has_token = !empty($row['fcm_token']);
    echo json_encode([
        'status' => 'success',
        'user_id' => $row['user_id'],
        'username' => $row['username'],
        'has_fcm_token' => $has_token,
        'token_length' => $has_token ? strlen($row['fcm_token']) : 0,
        'message' => $has_token ? 'User has FCM token' : 'User does not have FCM token - app needs to register'
    ]);
} else {
    echo json_encode(['status' => 'error', 'message' => 'User not found']);
}

$stmt->close();
$conn->close();
?>

