<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

// Select recipes and sort by created_at DESC (Newest first)
$sql = "SELECT recipe_id, title, description, tags, images, created_at FROM Recipes ORDER BY created_at DESC";
$result = $conn->query($sql);

$recipes = array();

if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        // Just send the data as is. 
        // The Android app will handle adding the full IP address to the image path.
        $recipes[] = $row;
    }

    echo json_encode(["status" => "success", "data" => $recipes]);
}
else{

    echo json_encode(["status" => "no_recipe", "message" => "No recipes found"]);
}



$conn->close();
?>