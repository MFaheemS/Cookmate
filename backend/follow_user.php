<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

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
    echo json_encode(["status" => "success", "message" => "Successfully followed user"]);
} else {
    echo json_encode(["status" => "error", "message" => "Failed to follow user"]);
}

$stmt->close();
$checkStmt->close();
$conn->close();
?>

