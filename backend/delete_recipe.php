<?php
header('Content-Type: application/json');
require_once 'db_connect.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $recipe_id = isset($_POST['recipe_id']) ? intval($_POST['recipe_id']) : 0;

    if ($user_id <= 0 || $recipe_id <= 0) {
        echo json_encode(['status' => 'error', 'message' => 'Invalid parameters']);
        exit();
    }

    // Verify that the recipe belongs to the user
    $checkSql = "SELECT user_id, images FROM Recipes WHERE recipe_id = ?";
    $checkStmt = $conn->prepare($checkSql);
    $checkStmt->bind_param("i", $recipe_id);
    $checkStmt->execute();
    $result = $checkStmt->get_result();

    if ($result->num_rows == 0) {
        echo json_encode(['status' => 'error', 'message' => 'Recipe not found']);
        exit();
    }

    $recipe = $result->fetch_assoc();
    if ($recipe['user_id'] != $user_id) {
        echo json_encode(['status' => 'error', 'message' => 'You do not have permission to delete this recipe']);
        exit();
    }

    // Delete the recipe image file if exists
    $imagePath = $recipe['images'];
    if (!empty($imagePath) && file_exists($imagePath)) {
        unlink($imagePath);
    }

    // Delete the recipe from database (cascades will delete related entries)
    $deleteSql = "DELETE FROM Recipes WHERE recipe_id = ? AND user_id = ?";
    $deleteStmt = $conn->prepare($deleteSql);
    $deleteStmt->bind_param("ii", $recipe_id, $user_id);

    if ($deleteStmt->execute()) {
        echo json_encode(['status' => 'success', 'message' => 'Recipe deleted successfully']);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Failed to delete recipe']);
    }

    $checkStmt->close();
    $deleteStmt->close();
    $conn->close();
} else {
    echo json_encode(['status' => 'error', 'message' => 'Invalid request method']);
}
?>

