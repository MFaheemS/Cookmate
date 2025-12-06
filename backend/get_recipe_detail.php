<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

if (!isset($_POST['recipe_id'])) {
    echo json_encode(["status" => "error", "message" => "Recipe ID Missing"]);
    exit();
}

$recipe_id = $_POST['recipe_id'];

// Modified Query: Adds two subqueries to count Favorites and Downloads
$sql = "SELECT 
            r.recipe_id, r.title, r.description, r.ingredients, r.steps, r.tags, r.images, r.created_at,
            u.username, u.profile_image,
            (SELECT COUNT(*) FROM Favorites WHERE recipe_id = r.recipe_id) as favorites_count,
            (SELECT COUNT(*) FROM Downloads WHERE recipe_id = r.recipe_id) as downloads_count
        FROM Recipes r
        JOIN Users u ON r.user_id = u.user_id
        WHERE r.recipe_id = ?";

$stmt = $conn->prepare($sql);
$stmt->bind_param("i", $recipe_id);
$stmt->execute();
$result = $stmt->get_result();

if ($row = $result->fetch_assoc()) {
    echo json_encode(["status" => "success", "data" => $row]);
} else {
    echo json_encode(["status" => "error", "message" => "Recipe not found"]);
}

$stmt->close();
$conn->close();
?>