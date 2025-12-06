<?php
header("Content-Type: application/json");
include "db_connect.php";

$response = array();

// Ensure at least username is provided (to identify who to update)
if (isset($_POST['original_username'])) {
    
    // We use 'original_username' to find the user, in case they change their username field
    $original_username = mysqli_real_escape_string($conn, $_POST['original_username']);
    
    // Collect new data
    $new_username = isset($_POST['username']) ? mysqli_real_escape_string($conn, $_POST['username']) : null;
    $email        = isset($_POST['email']) ? mysqli_real_escape_string($conn, $_POST['email']) : null;
    $password     = isset($_POST['password']) ? mysqli_real_escape_string($conn, $_POST['password']) : null;
    
    // 1. Build the Dynamic Query (Only update fields that are sent)
    $updateFields = [];
    if ($new_username) $updateFields[] = "username = '$new_username'";
    if ($email)        $updateFields[] = "email = '$email'";
    if ($password)     $updateFields[] = "password = '$password'"; // In production, hash this!

    // 2. Handle Image Upload (Optional)
    if (!empty($_POST['profile_image'])) {
        $profileImg = $_POST['profile_image'];
        $imageName = uniqid() . '.png';
        $uploadDir = 'uploads/';
        if (!is_dir($uploadDir)) mkdir($uploadDir, 0777, true);
        
        $uploadPath = $uploadDir . $imageName;
        
        if (strpos($profileImg, 'data:image') === 0) {
            $profileImg = explode(',', $profileImg)[1];
        }
        $decodedImage = base64_decode($profileImg);
        
        if ($decodedImage !== false) {
            file_put_contents($uploadPath, $decodedImage);
            $updateFields[] = "profile_image = '$uploadPath'";
        }
    }

    if (count($updateFields) > 0) {
        $sql = "UPDATE Users SET " . implode(", ", $updateFields) . " WHERE username = '$original_username'";
        
        if (mysqli_query($conn, $sql)) {
            $response['status'] = 1;
            $response['message'] = "Profile updated successfully";
        } else {
            $response['status'] = 0;
            $response['message'] = "Database error: " . mysqli_error($conn);
        }
    } else {
        $response['status'] = 0;
        $response['message'] = "No changes detected.";
    }

} else {
    $response['status'] = 0;
    $response['message'] = "Missing original username.";
}

echo json_encode($response);
?>