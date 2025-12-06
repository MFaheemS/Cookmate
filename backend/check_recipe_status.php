<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
$recipe_id = isset($_GET['recipe_id']) ? intval($_GET['recipe_id']) : 0;

if ($user_id == 0 || $recipe_id == 0) {
    echo json_encode([
        "status" => "error",
        "message" => "Invalid parameters",
        "is_liked" => false,
        "is_downloaded" => false
    ]);
    exit();
}

// Check if liked
$likeSql = "SELECT like_id FROM Likes WHERE user_id = ? AND recipe_id = ?";
$likeStmt = $conn->prepare($likeSql);
$likeStmt->bind_param("ii", $user_id, $recipe_id);
$likeStmt->execute();
$likeResult = $likeStmt->get_result();
$isLiked = $likeResult->num_rows > 0;

// Check if downloaded
$downloadSql = "SELECT download_id FROM Downloads WHERE user_id = ? AND recipe_id = ?";
$downloadStmt = $conn->prepare($downloadSql);
$downloadStmt->bind_param("ii", $user_id, $recipe_id);
$downloadStmt->execute();
$downloadResult = $downloadStmt->get_result();
$isDownloaded = $downloadResult->num_rows > 0;

// Get like count
$likeCountSql = "SELECT COUNT(*) as count FROM Likes WHERE recipe_id = ?";
$likeCountStmt = $conn->prepare($likeCountSql);
$likeCountStmt->bind_param("i", $recipe_id);
$likeCountStmt->execute();
$likeCountResult = $likeCountStmt->get_result();
$likeCountData = $likeCountResult->fetch_assoc();

// Get download count
$downloadCountSql = "SELECT COUNT(*) as count FROM Downloads WHERE recipe_id = ?";
$downloadCountStmt = $conn->prepare($downloadCountSql);
$downloadCountStmt->bind_param("i", $recipe_id);
$downloadCountStmt->execute();
$downloadCountResult = $downloadCountStmt->get_result();
$downloadCountData = $downloadCountResult->fetch_assoc();

echo json_encode([
    "status" => "success",
    "is_liked" => $isLiked,
    "is_downloaded" => $isDownloaded,
    "like_count" => $likeCountData['count'],
    "download_count" => $downloadCountData['count']
]);

$likeStmt->close();
$downloadStmt->close();
$likeCountStmt->close();
$downloadCountStmt->close();
$conn->close();
?>

