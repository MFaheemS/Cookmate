<?php
// Test file to verify delete_recipe.php is accessible
header('Content-Type: application/json');

echo json_encode([
    'status' => 'success',
    'message' => 'delete_recipe.php is accessible',
    'timestamp' => date('Y-m-d H:i:s'),
    'post_data' => $_POST,
    'get_data' => $_GET
]);
?>

