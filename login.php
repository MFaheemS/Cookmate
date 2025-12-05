<?php
header("Content-Type: application/json");
include "db_connect.php";

$response = array();

// Check required fields
if (isset($_POST['username']) && isset($_POST['password'])) {

    $username = mysqli_real_escape_string($conn, $_POST['username']);
    $password = mysqli_real_escape_string($conn, $_POST['password']);

    // Query user
    $query = "SELECT * FROM Users WHERE username = '$username' AND password = '$password' LIMIT 1";
    $result = mysqli_query($conn, $query);

    if ($result && mysqli_num_rows($result) == 1) {

        $user = mysqli_fetch_assoc($result);

        // Success
        $response['status'] = 1;
        $response['message'] = "Login successful.";

        // Return user data to app
        $response['user'] = array(
            "user_id"       => $user['user_id'],
            "username"      => $user['username'],
            "first_name"    => $user['first_name'],
            "last_name"     => $user['last_name'],
            "email"         => $user['email'],
            "profile_image" => $user['profile_image']
        );
    } 
    else {
        // Invalid login
        $response['status'] = 0;
        $response['message'] = "Invalid username or password.";
    }

} else {
    $response['status'] = 0;
    $response['message'] = "Missing required fields.";
}

echo json_encode($response);
?>
