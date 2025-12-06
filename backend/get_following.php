<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
$query = isset($_GET['query']) ? $_GET['query'] : '';

if ($user_id == 0) {
    echo json_encode(["status" => "error", "message" => "Invalid user ID"]);
    exit();
}

// Get users that this user is following
$searchTerm = $query . "%";

$sql = "SELECT u.user_id, u.username, u.first_name, u.last_name, u.profile_image
        FROM Followers f
        JOIN Users u ON f.following_id = u.user_id
        WHERE f.follower_id = ?
        AND (u.username LIKE ? OR u.first_name LIKE ? OR u.last_name LIKE ?)
        ORDER BY f.created_at DESC";

$stmt = $conn->prepare($sql);
$stmt->bind_param("isss", $user_id, $searchTerm, $searchTerm, $searchTerm);
$stmt->execute();
$result = $stmt->get_result();

$following = array();
while($row = $result->fetch_assoc()) {
    $following[] = $row;
}

echo json_encode(["status" => "success", "data" => $following]);

$stmt->close();
$conn->close();
?>

