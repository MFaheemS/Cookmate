<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;

if ($user_id == 0) {
    echo json_encode(["status" => "error", "message" => "Invalid user ID"]);
    exit();
}

// Get user profile information
$sql = "SELECT user_id, username, first_name, last_name, profile_image, created_at
        FROM Users
        WHERE user_id = ?";

$stmt = $conn->prepare($sql);
$stmt->bind_param("i", $user_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows == 0) {
    echo json_encode(["status" => "error", "message" => "User not found"]);
    exit();
}

$user = $result->fetch_assoc();

// Get follower count
$sqlFollowers = "SELECT COUNT(*) as followers_count FROM Followers WHERE following_id = ?";
$stmtFollowers = $conn->prepare($sqlFollowers);
$stmtFollowers->bind_param("i", $user_id);
$stmtFollowers->execute();
$resultFollowers = $stmtFollowers->get_result();
$followersData = $resultFollowers->fetch_assoc();
$user['followers_count'] = $followersData['followers_count'];

// Get following count
$sqlFollowing = "SELECT COUNT(*) as following_count FROM Followers WHERE follower_id = ?";
$stmtFollowing = $conn->prepare($sqlFollowing);
$stmtFollowing->bind_param("i", $user_id);
$stmtFollowing->execute();
$resultFollowing = $stmtFollowing->get_result();
$followingData = $resultFollowing->fetch_assoc();
$user['following_count'] = $followingData['following_count'];

// Get uploads count
$sqlUploads = "SELECT COUNT(*) as uploads_count FROM Recipes WHERE user_id = ?";
$stmtUploads = $conn->prepare($sqlUploads);
$stmtUploads->bind_param("i", $user_id);
$stmtUploads->execute();
$resultUploads = $stmtUploads->get_result();
$uploadsData = $resultUploads->fetch_assoc();
$user['uploads_count'] = $uploadsData['uploads_count'];

// Get user's recipes
$sqlRecipes = "SELECT recipe_id, title, description, tags, images, created_at
               FROM Recipes
               WHERE user_id = ?
               ORDER BY created_at DESC";

$stmtRecipes = $conn->prepare($sqlRecipes);
$stmtRecipes->bind_param("i", $user_id);
$stmtRecipes->execute();
$resultRecipes = $stmtRecipes->get_result();

$recipes = array();
while($row = $resultRecipes->fetch_assoc()) {
    $recipes[] = $row;
}

$user['recipes'] = $recipes;

echo json_encode(["status" => "success", "data" => $user]);

$stmt->close();
$stmtFollowers->close();
$stmtFollowing->close();
$stmtUploads->close();
$stmtRecipes->close();
$conn->close();
?>

