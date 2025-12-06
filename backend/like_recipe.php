<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$recipe_id = isset($_POST['recipe_id']) ? intval($_POST['recipe_id']) : 0;

if ($user_id == 0 || $recipe_id == 0) {
    echo json_encode(["status" => "error", "message" => "Invalid parameters"]);
    exit();
}

// Check if already liked
$checkSql = "SELECT like_id FROM Likes WHERE user_id = ? AND recipe_id = ?";
$checkStmt = $conn->prepare($checkSql);
$checkStmt->bind_param("ii", $user_id, $recipe_id);
$checkStmt->execute();
$checkResult = $checkStmt->get_result();

if ($checkResult->num_rows > 0) {
    echo json_encode(["status" => "error", "message" => "Already liked"]);
    exit();
}

// Insert like
$sql = "INSERT INTO Likes (user_id, recipe_id) VALUES (?, ?)";
$stmt = $conn->prepare($sql);
$stmt->bind_param("ii", $user_id, $recipe_id);

if ($stmt->execute()) {
    // Get updated like count
    $countSql = "SELECT COUNT(*) as count FROM Likes WHERE recipe_id = ?";
    $countStmt = $conn->prepare($countSql);
    $countStmt->bind_param("i", $recipe_id);
    $countStmt->execute();
    $countResult = $countStmt->get_result();
    $countData = $countResult->fetch_assoc();

    echo json_encode([
        "status" => "success",
        "message" => "Recipe liked",
        "like_count" => $countData['count']
    ]);
    $countStmt->close();
} else {
    echo json_encode(["status" => "error", "message" => "Failed to like recipe"]);
}

$stmt->close();
$checkStmt->close();
$conn->close();
?>

