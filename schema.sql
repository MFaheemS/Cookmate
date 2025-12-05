CREATE TABLE Users (
    user_id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    profile_image VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


CREATE TABLE Recipes (
    recipe_id INT AUTO_INCREMENT PRIMARY KEY NOT NULL, -- Server ID
    unique_id VARCHAR(36) NOT NULL UNIQUE,            -- APP Generated UUID (Vital for offline sync)
    user_id INT NOT NULL,
    
    -- Content
    title VARCHAR(200) NOT NULL,
    
    
    -- Storing complex lists as JSON is best for Sync/Performance in this case
    ingredients JSON,      -- Stores: [{"name":"Rice", "qty":"1 cup"}]
    steps JSON,            -- Stores: ["Step 1...", "Step 2..."]
    tags VARCHAR(255),     -- Stores: "#Lunch, #Hot"
    
    -- Images
    images VARCHAR(255),           -- Stores: ["uploads/img1.jpg", "uploads/img2.jpg"]
    
    -- Meta
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
);
