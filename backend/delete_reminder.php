<?php
header('Content-Type: application/json');
require_once 'connection.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $recipe_id = isset($_POST['recipe_id']) ? intval($_POST['recipe_id']) : 0;

    if ($user_id <= 0 || $recipe_id <= 0) {
        echo json_encode(['status' => 'error', 'message' => 'Invalid parameters']);
        exit();
    }

    $sql = "DELETE FROM Reminders WHERE user_id = ? AND recipe_id = ?";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("ii", $user_id, $recipe_id);

    if ($stmt->execute()) {
        if ($stmt->affected_rows > 0) {
            echo json_encode(['status' => 'success', 'message' => 'Reminder deleted']);
        } else {
            echo json_encode(['status' => 'error', 'message' => 'Reminder not found']);
        }
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Failed to delete reminder']);
    }

    $stmt->close();
    $conn->close();
} else {
    echo json_encode(['status' => 'error', 'message' => 'Invalid request method']);
}
?>

