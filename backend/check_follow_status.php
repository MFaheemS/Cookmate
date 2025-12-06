<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

$follower_id = isset($_GET['follower_id']) ? intval($_GET['follower_id']) : 0;
$following_id = isset($_GET['following_id']) ? intval($_GET['following_id']) : 0;

if ($follower_id == 0 || $following_id == 0) {
    echo json_encode(["status" => "error", "message" => "Invalid user IDs", "is_following" => false]);
    exit();
}

// Check if following
$sql = "SELECT follow_id FROM Followers WHERE follower_id = ? AND following_id = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("ii", $follower_id, $following_id);
$stmt->execute();
$result = $stmt->get_result();

$isFollowing = $result->num_rows > 0;

echo json_encode(["status" => "success", "is_following" => $isFollowing]);

$stmt->close();
$conn->close();
?>

