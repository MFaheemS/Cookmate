<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';
require_once 'send_notification.php';

$follower_id = isset($_POST['follower_id']) ? intval($_POST['follower_id']) : 0;
$following_id = isset($_POST['following_id']) ? intval($_POST['following_id']) : 0;

if ($follower_id == 0 || $following_id == 0) {
    echo json_encode(["status" => "error", "message" => "Invalid user IDs"]);
    exit();
}

// Check if already following
$checkSql = "SELECT follow_id FROM Followers WHERE follower_id = ? AND following_id = ?";
$checkStmt = $conn->prepare($checkSql);
$checkStmt->bind_param("ii", $follower_id, $following_id);
$checkStmt->execute();
$checkResult = $checkStmt->get_result();

if ($checkResult->num_rows > 0) {
    echo json_encode(["status" => "error", "message" => "Already following this user"]);
    exit();
}

// Insert follow relationship
$sql = "INSERT INTO Followers (follower_id, following_id) VALUES (?, ?)";
$stmt = $conn->prepare($sql);
$stmt->bind_param("ii", $follower_id, $following_id);

if ($stmt->execute()) {
    // Get follower's username
    $userSql = "SELECT username FROM Users WHERE user_id = ?";
    $userStmt = $conn->prepare($userSql);
    $userStmt->bind_param("i", $follower_id);
    $userStmt->execute();
    $userResult = $userStmt->get_result();
    $followerData = $userResult->fetch_assoc();
    $followerUsername = $followerData['username'] ?? 'Someone';

    // Try to send notification to the followed user
    try {
        notifyNewFollower($following_id, $followerUsername);
    } catch (Exception $e) {
        error_log("Failed to send follower notification: " . $e->getMessage());
    }

    echo json_encode(["status" => "success", "message" => "Successfully followed user"]);
    $userStmt->close();
} else {
    echo json_encode(["status" => "error", "message" => "Failed to follow user"]);
}

$stmt->close();
$checkStmt->close();
$conn->close();
?>

