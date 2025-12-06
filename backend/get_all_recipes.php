<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;

// Select recipes with like/download counts and user's status
if ($user_id > 0) {
    $sql = "SELECT r.recipe_id, r.title, r.description, r.tags, r.images, r.created_at, r.user_id,
            (SELECT COUNT(*) FROM Likes WHERE recipe_id = r.recipe_id) as like_count,
            (SELECT COUNT(*) FROM Downloads WHERE recipe_id = r.recipe_id) as download_count,
            (SELECT COUNT(*) FROM Likes WHERE recipe_id = r.recipe_id AND user_id = ?) as is_liked,
            (SELECT COUNT(*) FROM Downloads WHERE recipe_id = r.recipe_id AND user_id = ?) as is_downloaded
            FROM Recipes r
            ORDER BY r.created_at DESC";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("ii", $user_id, $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
} else {
    // If no user_id, just get recipes with counts but no status
    $sql = "SELECT r.recipe_id, r.title, r.description, r.tags, r.images, r.created_at, r.user_id,
            (SELECT COUNT(*) FROM Likes WHERE recipe_id = r.recipe_id) as like_count,
            (SELECT COUNT(*) FROM Downloads WHERE recipe_id = r.recipe_id) as download_count,
            0 as is_liked,
            0 as is_downloaded
            FROM Recipes r
            ORDER BY r.created_at DESC";

    $result = $conn->query($sql);
}

$recipes = array();

if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $recipe = array(
            "recipe_id" => $row['recipe_id'],
            "title" => $row['title'],
            "description" => $row['description'],
            "tags" => $row['tags'],
            "images" => $row['images'],
            "created_at" => $row['created_at'],
            "user_id" => $row['user_id'],
            "like_count" => $row['like_count'],
            "download_count" => $row['download_count'],
            "is_liked" => $row['is_liked'] > 0,
            "is_downloaded" => $row['is_downloaded'] > 0
        );
        $recipes[] = $recipe;
    }

    echo json_encode(["status" => "success", "data" => $recipes]);
} else {
    echo json_encode(["status" => "no_recipe", "message" => "No recipes found"]);
}

if (isset($stmt)) {
    $stmt->close();
}
$conn->close();
?>