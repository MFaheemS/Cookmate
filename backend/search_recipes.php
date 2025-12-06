<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

$query = isset($_GET['query']) ? $_GET['query'] : '';

// If query is empty, return nothing or all (let's return nothing to keep UI clean)
if (trim($query) == '') {
    echo json_encode(["status" => "success", "data" => []]);
    exit();
}

// Search for Title containing the query (Case insensitive usually in MySQL)
$searchTerm = "%" . $query . "%";
$sql = "SELECT recipe_id, title, description, tags, images FROM Recipes WHERE title LIKE ?";

$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $searchTerm);
$stmt->execute();
$result = $stmt->get_result();

$recipes = array();
while($row = $result->fetch_assoc()) {
    $recipes[] = $row;
}

echo json_encode(["status" => "success", "data" => $recipes]);

$stmt->close();
$conn->close();
?>