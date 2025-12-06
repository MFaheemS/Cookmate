<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

$follower_id = isset($_POST['follower_id']) ? intval($_POST['follower_id']) : 0;
$following_id = isset($_POST['following_id']) ? intval($_POST['following_id']) : 0;

if ($follower_id == 0 || $following_id == 0) {
    echo json_encode(["status" => "error", "message" => "Invalid user IDs"]);
    exit();
}

// Delete follow relationship
$sql = "DELETE FROM Followers WHERE follower_id = ? AND following_id = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("ii", $follower_id, $following_id);

if ($stmt->execute()) {
    if ($stmt->affected_rows > 0) {
        echo json_encode(["status" => "success", "message" => "Successfully unfollowed user"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Not following this user"]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Failed to unfollow user"]);
}

$stmt->close();
$conn->close();
?>

