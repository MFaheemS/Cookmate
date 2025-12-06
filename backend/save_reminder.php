<?php
header('Content-Type: application/json');
require_once 'connection.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $recipe_id = isset($_POST['recipe_id']) ? intval($_POST['recipe_id']) : 0;
    $reminder_time = isset($_POST['reminder_time']) ? $_POST['reminder_time'] : 0;

    if ($user_id <= 0 || $recipe_id <= 0 || $reminder_time <= 0) {
        echo json_encode(['status' => 'error', 'message' => 'Invalid parameters']);
        exit();
    }

    // Check if reminder already exists
    $check_sql = "SELECT reminder_id FROM Reminders WHERE user_id = ? AND recipe_id = ?";
    $check_stmt = $conn->prepare($check_sql);
    $check_stmt->bind_param("ii", $user_id, $recipe_id);
    $check_stmt->execute();
    $result = $check_stmt->get_result();

    if ($result->num_rows > 0) {
        // Update existing reminder
        $update_sql = "UPDATE Reminders SET reminder_time = ? WHERE user_id = ? AND recipe_id = ?";
        $update_stmt = $conn->prepare($update_sql);
        $update_stmt->bind_param("sii", $reminder_time, $user_id, $recipe_id);

        if ($update_stmt->execute()) {
            echo json_encode(['status' => 'success', 'message' => 'Reminder updated']);
        } else {
            echo json_encode(['status' => 'error', 'message' => 'Failed to update reminder']);
        }
        $update_stmt->close();
    } else {
        // Insert new reminder
        $insert_sql = "INSERT INTO Reminders (user_id, recipe_id, reminder_time) VALUES (?, ?, ?)";
        $insert_stmt = $conn->prepare($insert_sql);
        $insert_stmt->bind_param("iis", $user_id, $recipe_id, $reminder_time);

        if ($insert_stmt->execute()) {
            echo json_encode(['status' => 'success', 'message' => 'Reminder created']);
        } else {
            echo json_encode(['status' => 'error', 'message' => 'Failed to create reminder']);
        }
        $insert_stmt->close();
    }

    $check_stmt->close();
    $conn->close();
} else {
    echo json_encode(['status' => 'error', 'message' => 'Invalid request method']);
}
?>

