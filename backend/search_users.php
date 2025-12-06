<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

$query = isset($_GET['query']) ? $_GET['query'] : '';
$current_user_id = isset($_GET['current_user_id']) ? intval($_GET['current_user_id']) : 0;

// If query is empty, return nothing
if (trim($query) == '') {
    echo json_encode(["status" => "success", "data" => []]);
    exit();
}

// Search for username or first_name or last_name starting with the query (not containing)
$searchTerm = $query . "%";
$sql = "SELECT user_id, username, first_name, last_name, profile_image
        FROM Users
        WHERE (username LIKE ? OR first_name LIKE ? OR last_name LIKE ?)
        AND user_id != ?
        LIMIT 20";

$stmt = $conn->prepare($sql);
$stmt->bind_param("sssi", $searchTerm, $searchTerm, $searchTerm, $current_user_id);
$stmt->execute();
$result = $stmt->get_result();

$users = array();
while($row = $result->fetch_assoc()) {
    $users[] = $row;
}

echo json_encode(["status" => "success", "data" => $users]);

$stmt->close();
$conn->close();
?>

