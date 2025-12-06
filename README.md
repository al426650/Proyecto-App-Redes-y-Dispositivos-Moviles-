# Proyecto App Redes y Dispositivos Moviles
Final project for "Redes y Dispositivos Móviles" – last year of Bachelor's in Intelligent Robotics at Universitat Jaume I. Meal planner app with weekly menu generator, smart shopping lists (excludes pantry items), and AI-powered ingredient detection from fridge photos.

## Features

- **Weekly Menu Planner**: Create structured meal plans from a recipe database
- **Smart Shopping Lists**: Auto-generated lists that skip ingredients already in your pantry
- **Pantry Management**: Track available food items to prevent waste
- **AI Vision**: Upload fridge photos to our web server for automatic ingredient detection
- **Recipe Database**: Centralized storage for all your recipes

## Technologies

- **Mobile App**: React Native / Flutter / Android (Kotlin)
- **Backend**: Node.js / Python (Flask/FastAPI)
- **Database**: SQLite / PostgreSQL
- **AI/ML**: TensorFlow / PyTorch (Computer Vision model)
- **Web Server**: Express / FastAPI

## Installation

```bash
# Clone the repository
git clone https://github.com/yourusername/intellimeal.git
cd intellimeal

# Install dependencies
# [Add specific commands based on your tech stack]

# Run the application
# [Add specific commands]
```

## Usage

1. Register your available ingredients in the Pantry section
2. Create your weekly menu by selecting recipes
3. Generate your smart shopping list (automatically excludes pantry items)
4. Optional: Take a photo of your fridge and upload it to auto-update your pantry

## Project Structure

```
/intellimeal
├── /mobile-app          # Mobile application
├── /backend             # API server
├── /ml-model            # AI model for image processing
├── /database            # Database schema and migrations
└── README.md
```

## License

MIT License – see LICENSE file for details.

## Authors

- [Jorge Castro](https://github.com/al426650)
- [Mario López](https://github.com/member2)
- [David Ballester](https://github.com/member3)

*Academic project – Universitat Jaume I, 2024*
