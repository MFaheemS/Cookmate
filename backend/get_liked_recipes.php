<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;

if ($user_id == 0) {
    echo json_encode(["status" => "error", "message" => "Invalid user ID"]);
    exit();
}

// Get liked recipes with their details
$sql = "SELECT r.recipe_id, r.title, r.description, r.tags, r.images, r.user_id,
        (SELECT COUNT(*) FROM Likes WHERE recipe_id = r.recipe_id) as like_count,
        (SELECT COUNT(*) FROM Downloads WHERE recipe_id = r.recipe_id) as download_count,
        (SELECT COUNT(*) FROM Likes WHERE recipe_id = r.recipe_id AND user_id = ?) as is_liked,
        (SELECT COUNT(*) FROM Downloads WHERE recipe_id = r.recipe_id AND user_id = ?) as is_downloaded
        FROM Recipes r
        INNER JOIN Likes l ON r.recipe_id = l.recipe_id
        WHERE l.user_id = ?
        ORDER BY l.created_at DESC";

$stmt = $conn->prepare($sql);
$stmt->bind_param("iii", $user_id, $user_id, $user_id);
$stmt->execute();
$result = $stmt->get_result();

$recipes = array();
while ($row = $result->fetch_assoc()) {
    $recipe = array(
        "recipe_id" => $row['recipe_id'],
        "title" => $row['title'],
        "description" => $row['description'],
        "tags" => $row['tags'],
        "images" => $row['images'],
        "user_id" => $row['user_id'],
        "like_count" => $row['like_count'],
        "download_count" => $row['download_count'],
        "is_liked" => $row['is_liked'] > 0,
        "is_downloaded" => $row['is_downloaded'] > 0
    );
    array_push($recipes, $recipe);
}

echo json_encode([
    "status" => "success",
    "data" => $recipes,
    "count" => count($recipes)
]);

$stmt->close();
$conn->close();
?>

