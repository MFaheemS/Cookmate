<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
$viewer_id = isset($_GET['viewer_id']) ? intval($_GET['viewer_id']) : 0;

if ($user_id == 0) {
    echo json_encode(["status" => "error", "message" => "Invalid user ID"]);
    exit();
}

// Get user profile information including privacy setting
$sql = "SELECT user_id, username, first_name, last_name, profile_image, created_at, is_private
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
$is_private = isset($user['is_private']) ? (bool)$user['is_private'] : false;
$user['is_private'] = $is_private;

// Check if viewer can access this profile
// User can ALWAYS view their own profile
$can_view = true;
$is_own_profile = ($viewer_id == $user_id);

if ($is_private && !$is_own_profile && $viewer_id != 0) {
    // Profile is private, viewer is not the owner
    // Check if viewer is already following this user
    $sqlCheckFollow = "SELECT COUNT(*) as is_following FROM Followers WHERE follower_id = ? AND following_id = ?";
    $stmtCheckFollow = $conn->prepare($sqlCheckFollow);
    $stmtCheckFollow->bind_param("ii", $viewer_id, $user_id);
    $stmtCheckFollow->execute();
    $resultCheckFollow = $stmtCheckFollow->get_result();
    $followData = $resultCheckFollow->fetch_assoc();

    $can_view = $followData['is_following'] > 0;
    $stmtCheckFollow->close();
}

$user['can_view'] = $can_view;
$user['is_own_profile'] = $is_own_profile;

// If cannot view (private and not following), return limited profile info
if (!$can_view && !$is_own_profile) {
    echo json_encode(["status" => "success", "data" => $user]);
    $stmt->close();
    $conn->close();
    exit();
}

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

