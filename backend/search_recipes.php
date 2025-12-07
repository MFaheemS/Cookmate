<?php
header('Content-Type: application/json; charset=utf-8');
require 'db_connect.php';

$query = isset($_GET['query']) ? $_GET['query'] : '';
$categories = isset($_GET['categories']) ? $_GET['categories'] : '';
$ingredients = isset($_GET['ingredients']) ? $_GET['ingredients'] : '';

// Build SQL query dynamically
$conditions = array();
$params = array();
$types = '';

// Add text search condition if query is provided
if (!empty(trim($query))) {
    $conditions[] = "title LIKE ?";
    $params[] = "%" . $query . "%";
    $types .= 's';
}

// Add category conditions if categories are provided (AND logic - must match ALL categories)
if (!empty($categories)) {
    $categoryArray = array_map('trim', explode(',', $categories));

    foreach ($categoryArray as $category) {
        // Remove # if present and make case-insensitive search
        $cleanCategory = ltrim($category, '#');

        // Each category must be present (AND logic)
        $conditions[] = "LOWER(tags) LIKE LOWER(?)";
        $params[] = "%#" . $cleanCategory . "%";
        $types .= 's';
    }
}

// Add ingredient conditions if ingredients are provided (AND logic - must contain ALL ingredients)
if (!empty($ingredients)) {
    $ingredientArray = array_map('trim', explode(',', $ingredients));

    foreach ($ingredientArray as $ingredient) {
        // Each ingredient must be present in the ingredients field (AND logic)
        $conditions[] = "LOWER(ingredients) LIKE LOWER(?)";
        $params[] = "%" . $ingredient . "%";
        $types .= 's';
    }
}

// If no conditions, return empty results
if (empty($conditions)) {
    echo json_encode(["status" => "success", "data" => []]);
    exit();
}

// Build final SQL with all conditions joined by AND
$sql = "SELECT recipe_id, title, description, tags, images FROM Recipes WHERE " . implode(' AND ', $conditions);

$stmt = $conn->prepare($sql);
if (!empty($params)) {
    $stmt->bind_param($types, ...$params);
}
$stmt->execute();
$result = $stmt->get_result();

$recipes = array();
while($row = $result->fetch_assoc()) {
    $recipes[] = $row;
}

echo json_encode(["status" => "success", "data" => $recipes]);

$stmt->close();
$conn->close();
?>