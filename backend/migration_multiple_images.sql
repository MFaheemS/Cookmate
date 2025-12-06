-- Update Recipes table to support multiple images stored as JSON
ALTER TABLE Recipes MODIFY COLUMN images JSON;

-- Note: The images field will now store an array of image paths
-- Example: ["uploads/img1.jpg", "uploads/img2.jpg", "uploads/img3.jpg"]
-- The first image in the array is the cover/primary image

