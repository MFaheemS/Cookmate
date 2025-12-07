<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');

include 'connection.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $user_id = $_POST['user_id'];
    $is_private = $_POST['is_private'];

    if (empty($user_id)) {
        $response['status'] = 'error';
        $response['message'] = 'User ID is required';
        echo json_encode($response);
        exit;
    }

    // Update privacy setting
    $sql = "UPDATE users SET is_private = ? WHERE user_id = ?";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("ii", $is_private, $user_id);

    if ($stmt->execute()) {
        $response['status'] = 'success';
        $response['message'] = 'Privacy settings updated successfully';
    } else {
        $response['status'] = 'error';
        $response['message'] = 'Failed to update privacy settings';
    }

    $stmt->close();
} else {
    $response['status'] = 'error';
    $response['message'] = 'Invalid request method';
}

$conn->close();
echo json_encode($response);
?>

