<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$recipe_id = isset($_POST['recipe_id']) ? intval($_POST['recipe_id']) : 0;

if ($user_id == 0 || $recipe_id == 0) {
    echo json_encode(["status" => "error", "message" => "Invalid parameters"]);
    exit();
}

// Delete like
$sql = "DELETE FROM Likes WHERE user_id = ? AND recipe_id = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("ii", $user_id, $recipe_id);

if ($stmt->execute()) {
    if ($stmt->affected_rows > 0) {
        // Get updated like count
        $countSql = "SELECT COUNT(*) as count FROM Likes WHERE recipe_id = ?";
        $countStmt = $conn->prepare($countSql);
        $countStmt->bind_param("i", $recipe_id);
        $countStmt->execute();
        $countResult = $countStmt->get_result();
        $countData = $countResult->fetch_assoc();

        echo json_encode([
            "status" => "success",
            "message" => "Recipe unliked",
            "like_count" => $countData['count']
        ]);
        $countStmt->close();
    } else {
        echo json_encode(["status" => "error", "message" => "Recipe not liked"]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Failed to unlike recipe"]);
}

$stmt->close();
$conn->close();
?>

