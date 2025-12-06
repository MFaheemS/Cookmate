<?php
header('Content-Type: application/json');
require_once 'connection.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $fcm_token = isset($_POST['fcm_token']) ? $_POST['fcm_token'] : '';

    if ($user_id <= 0 || empty($fcm_token)) {
        echo json_encode(['status' => 'error', 'message' => 'Invalid parameters']);
        exit();
    }

    // Update user's FCM token
    $sql = "UPDATE Users SET fcm_token = ? WHERE user_id = ?";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("si", $fcm_token, $user_id);

    if ($stmt->execute()) {
        echo json_encode(['status' => 'success', 'message' => 'FCM token updated']);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Failed to update FCM token']);
    }

    $stmt->close();
    $conn->close();
} else {
    echo json_encode(['status' => 'error', 'message' => 'Invalid request method']);
}
?>

