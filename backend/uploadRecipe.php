<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';
require_once 'send_notification.php';

if ($_SERVER['REQUEST_METHOD'] != 'POST') {
    echo json_encode(["status" => "error", "message" => "Invalid Request Method"]);
    exit();
}

// 1. GET USERNAME INSTEAD OF ID
$username = isset($_POST['username']) ? $_POST['username'] : null;
$unique_id = isset($_POST['unique_id']) ? $_POST['unique_id'] : '';
$title = isset($_POST['title']) ? $_POST['title'] : '';
// ... (collect other fields: tags, ingredients, steps) ...
$tags = isset($_POST['tags']) ? $_POST['tags'] : '';
$ingredients = isset($_POST['ingredients']) ? $_POST['ingredients'] : '[]';
$steps = isset($_POST['steps']) ? $_POST['steps'] : '[]';

$description = isset($_POST['description']) ? $_POST['description'] : '';


if (!$username || !$title) {
    echo json_encode(["status" => "error", "message" => "Missing username or title"]);
    exit();
}

// 2. LOOKUP USER_ID FROM USERNAME
$user_id = null;
$stmt_user = $conn->prepare("SELECT user_id FROM Users WHERE username = ?");
$stmt_user->bind_param("s", $username);
$stmt_user->execute();
$result_user = $stmt_user->get_result();

if ($row = $result_user->fetch_assoc()) {
    $user_id = $row['user_id'];
}
$stmt_user->close();

if (!$user_id) {
    echo json_encode(["status" => "error", "message" => "User not found in database"]);
    exit();
}

// 3. HANDLE IMAGES (Multiple images support)
$image_paths = array();
if (isset($_POST['images']) && !empty($_POST['images'])) {
    $images_json = $_POST['images'];
    $images_array = json_decode($images_json, true);

    if (is_array($images_array)) {
        $target_dir = "media_uploads/";
        if (!file_exists($target_dir)) {
            mkdir($target_dir, 0777, true);
        }

        foreach ($images_array as $base64_string) {
            $image_data = base64_decode($base64_string);
            if ($image_data !== false) {
                $new_file_name = uniqid("img_", true) . ".jpg";
                $target_file = $target_dir . $new_file_name;
                if (file_put_contents($target_file, $image_data)) {
                    $image_paths[] = $target_file;
                }
            }
        }
    }
}

// Convert image paths array to JSON string for storage
$images_json_storage = json_encode($image_paths);

// 4. INSERT RECIPE (Using the found $user_id)
$stmt = $conn->prepare("INSERT INTO Recipes (unique_id, user_id, title, ingredients, steps, tags, images, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

if ($stmt) {
    // Note: We use the $user_id we found in step 2
    $stmt->bind_param("sissssss", $unique_id, $user_id, $title, $ingredients, $steps, $tags, $images_json_storage, $description);
    if ($stmt->execute()) {
        $recipe_id = $conn->insert_id;

        // Send notification to all followers about new recipe
        notifyFollowersAboutNewRecipe($user_id, $recipe_id, $title);

        echo json_encode(["status" => "success", "message" => "Recipe Uploaded"]);
    } else {
        echo json_encode(["status" => "error", "message" => "SQL Error: " . $stmt->error]);
    }
    $stmt->close();
} else {
    echo json_encode(["status" => "error", "message" => "Prep Error: " . $conn->error]);
}
$conn->close();
?>