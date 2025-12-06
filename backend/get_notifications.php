<?php
header('Content-Type: application/json');
require_once 'connection.php';

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
    $last_check = isset($_GET['last_check']) ? $_GET['last_check'] : '';

    if ($user_id <= 0) {
        echo json_encode(['status' => 'error', 'message' => 'Invalid user ID']);
        exit();
    }

    $notifications = array();

    // Get new followers since last check
    if (!empty($last_check)) {
        $sql = "SELECT f.follower_id, u.username, f.created_at
                FROM Followers f
                INNER JOIN Users u ON f.follower_id = u.user_id
                WHERE f.following_id = ? AND f.created_at > ?
                ORDER BY f.created_at DESC";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("is", $user_id, $last_check);
    } else {
        // Get last 5 followers if no timestamp provided
        $sql = "SELECT f.follower_id, u.username, f.created_at
                FROM Followers f
                INNER JOIN Users u ON f.follower_id = u.user_id
                WHERE f.following_id = ?
                ORDER BY f.created_at DESC
                LIMIT 5";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("i", $user_id);
    }

    $stmt->execute();
    $result = $stmt->get_result();

    while ($row = $result->fetch_assoc()) {
        $notifications[] = array(
            'type' => 'new_follower',
            'follower_id' => $row['follower_id'],
            'username' => $row['username'],
            'timestamp' => $row['created_at']
        );
    }

    $stmt->close();

    // Get new recipes from people user follows
    if (!empty($last_check)) {
        $recipeSql = "SELECT r.recipe_id, r.title, r.user_id, u.username, r.created_at
                      FROM Recipes r
                      INNER JOIN Followers f ON r.user_id = f.following_id
                      INNER JOIN Users u ON r.user_id = u.user_id
                      WHERE f.follower_id = ? AND r.created_at > ?
                      ORDER BY r.created_at DESC";
        $recipeStmt = $conn->prepare($recipeSql);
        $recipeStmt->bind_param("is", $user_id, $last_check);
    } else {
        // Get last 5 recipes if no timestamp
        $recipeSql = "SELECT r.recipe_id, r.title, r.user_id, u.username, r.created_at
                      FROM Recipes r
                      INNER JOIN Followers f ON r.user_id = f.following_id
                      INNER JOIN Users u ON r.user_id = u.user_id
                      WHERE f.follower_id = ?
                      ORDER BY r.created_at DESC
                      LIMIT 5";
        $recipeStmt = $conn->prepare($recipeSql);
        $recipeStmt->bind_param("i", $user_id);
    }

    $recipeStmt->execute();
    $recipeResult = $recipeStmt->get_result();

    while ($row = $recipeResult->fetch_assoc()) {
        $notifications[] = array(
            'type' => 'new_recipe',
            'recipe_id' => $row['recipe_id'],
            'recipe_title' => $row['title'],
            'author_id' => $row['user_id'],
            'author_username' => $row['username'],
            'timestamp' => $row['created_at']
        );
    }

    $recipeStmt->close();
    $conn->close();

    echo json_encode([
        'status' => 'success',
        'notifications' => $notifications,
        'count' => count($notifications)
    ]);
} else {
    echo json_encode(['status' => 'error', 'message' => 'Invalid request method']);
}
?>

