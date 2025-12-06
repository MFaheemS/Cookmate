<?php
header("Content-Type: application/json");

// Include database file
include "db_connect.php";

// Response array
$response = array();

// Check required fields
if (
    isset($_POST['username']) &&
    isset($_POST['first_name']) &&
    isset($_POST['last_name']) &&
    isset($_POST['email']) &&
    isset($_POST['password'])
) {

    $username   = mysqli_real_escape_string($conn, $_POST['username']);
    $firstName  = mysqli_real_escape_string($conn, $_POST['first_name']);
    $lastName   = mysqli_real_escape_string($conn, $_POST['last_name']);
    $email      = mysqli_real_escape_string($conn, $_POST['email']);
    $password   = mysqli_real_escape_string($conn, $_POST['password']);

    // Default profile image path (important!)
    $uploadPath = NULL;

    // Step 1: Check username
    $checkUsername = "SELECT user_id FROM Users WHERE username = '$username'";
    $resultUsername = mysqli_query($conn, $checkUsername);

    // Step 2: Check email
    $checkEmail = "SELECT user_id FROM Users WHERE email = '$email'";
    $resultEmail = mysqli_query($conn, $checkEmail);

    if ($resultUsername && mysqli_num_rows($resultUsername) > 0) {
        $response['status'] = 0;
        $response['message'] = "Username already exists.";
    }
    else if ($resultEmail && mysqli_num_rows($resultEmail) > 0) {
        $response['status'] = 0;
        $response['message'] = "Email already exists.";
    }
    else {

        // IF IMAGE IS SENT
        if (!empty($_POST['profile_image'])) {

            $profileImg = $_POST['profile_image'];
            $imageName = uniqid() . '.png';
            $uploadDir = 'uploads/';

            // Create folder if missing
            if (!is_dir($uploadDir)) {
                mkdir($uploadDir, 0777, true);
            }

            $uploadPath = $uploadDir . $imageName;

            // Remove base64 header if exists
            if (strpos($profileImg, 'data:image') === 0) {
                $profileImg = explode(',', $profileImg)[1];
            }

            // Decode and save
            $decodedImage = base64_decode($profileImg);

            if ($decodedImage !== false) {
                file_put_contents($uploadPath, $decodedImage);
            } else {
                // If decoding fails, do not save image
                $uploadPath = NULL;
            }
        }

        // Insert new user
        $insertQuery = "INSERT INTO Users 
            (username, first_name, last_name, email, password, profile_image)
            VALUES 
            ('$username', '$firstName', '$lastName', '$email', '$password', '$uploadPath')";

        if (mysqli_query($conn, $insertQuery)) {
            $userId = mysqli_insert_id($conn);

            $response['status'] = 1;
            $response['message'] = "Signup successful.";

            // Return user data
            $response['user'] = array(
                "user_id"       => $userId,
                "username"      => $username,
                "first_name"    => $firstName,
                "last_name"     => $lastName,
                "email"         => $email,
                "profile_image" => $uploadPath
            );
        } else {
            $response['status'] = 0;
            $response['message'] = "Database error: " . mysqli_error($conn);
        }
    }
} 
else {
    $response['status'] = 0;
    $response['message'] = "Missing required fields.";
}

echo json_encode($response);
?>
