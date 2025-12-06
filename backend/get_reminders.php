<?php
header('Content-Type: application/json');
require_once 'connection.php';

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;

    if ($user_id <= 0) {
        echo json_encode(['status' => 'error', 'message' => 'Invalid user ID']);
        exit();
    }

    // Get all reminders for the user with recipe details
    $sql = "SELECT DISTINCT r.reminder_id, r.recipe_id, r.reminder_time,
                   rec.title, rec.images
            FROM Reminders r
            INNER JOIN Recipes rec ON r.recipe_id = rec.recipe_id
            WHERE r.user_id = ?
            ORDER BY r.reminder_time ASC";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();

    $reminders = array();
    while ($row = $result->fetch_assoc()) {
        $reminders[] = array(
            'reminder_id' => $row['reminder_id'],
            'recipe_id' => $row['recipe_id'],
            'recipe_title' => $row['title'],
            'reminder_time' => $row['reminder_time'],
            'image_path' => $row['images']
        );
    }

    echo json_encode(['status' => 'success', 'data' => $reminders]);

    $stmt->close();
    $conn->close();
} else {
    echo json_encode(['status' => 'error', 'message' => 'Invalid request method']);
}
?>

