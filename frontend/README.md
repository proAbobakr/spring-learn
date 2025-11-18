# Location Rating Service - React Frontend

Modern React frontend for the Location Rating Service - a platform for discovering, rating, and reviewing locations.

## Features

- 🔐 **User Authentication** - Register, login, and manage user sessions with JWT
- 📍 **Location Discovery** - Browse, search, and filter locations by category or city
- ⭐ **Ratings & Reviews** - Rate locations (1-5 stars) and write detailed reviews
- 💬 **Comment System** - Nested comments with replies and like functionality
- 🖼️ **Image Gallery** - Upload and view location images with captions
- 🗺️ **Geolocation** - Find nearby locations based on your current position
- 📊 **Trending & Top Rated** - Discover popular and highly-rated locations
- 👤 **User Profile** - View your locations and reviews

## Tech Stack

- **React 18** - Modern UI library
- **React Router 6** - Client-side routing
- **Vite** - Fast build tool and dev server
- **Axios** - HTTP client for API calls
- **React Hook Form** - Form state management
- **React Leaflet** - Interactive maps
- **React Dropzone** - File upload handling
- **React Icons** - Icon library
- **React Toastify** - Toast notifications
- **Recharts** - Data visualization

## Prerequisites

- Node.js 16+ and npm/yarn
- Spring Boot backend running on `http://localhost:8080`

## Installation

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Create environment file:
```bash
cp .env.example .env
```

4. Configure environment variables in `.env`:
```env
VITE_API_BASE_URL=http://localhost:8080/api
```

## Development

Start the development server:
```bash
npm run dev
```

The app will be available at `http://localhost:3000`

## Building for Production

Build the production bundle:
```bash
npm run build
```

Preview the production build:
```bash
npm run preview
```

The built files will be in the `dist` directory.

## Project Structure

```
frontend/
├── public/              # Static assets
├── src/
│   ├── components/      # Reusable components
│   │   ├── Comment/     # Comment components
│   │   ├── Image/       # Image gallery components
│   │   ├── Layout/      # Header, Footer, etc.
│   │   ├── Location/    # Location card, list
│   │   ├── Rating/      # Rating components
│   │   └── PrivateRoute.jsx
│   ├── context/         # React Context providers
│   │   └── AuthContext.jsx
│   ├── hooks/           # Custom React hooks
│   │   └── useGeolocation.js
│   ├── pages/           # Page components
│   │   ├── Home.jsx
│   │   ├── Login.jsx
│   │   ├── Register.jsx
│   │   ├── LocationList.jsx
│   │   ├── LocationDetail.jsx
│   │   ├── LocationForm.jsx
│   │   └── Profile.jsx
│   ├── services/        # API service layer
│   │   ├── api.js
│   │   ├── authService.js
│   │   ├── locationService.js
│   │   ├── ratingService.js
│   │   ├── commentService.js
│   │   └── imageService.js
│   ├── utils/           # Utility functions
│   │   ├── constants.js
│   │   └── formatters.js
│   ├── App.jsx          # Main app component
│   ├── App.css          # Global styles
│   └── main.jsx         # Entry point
├── .env.example         # Environment variables template
├── package.json         # Dependencies
├── vite.config.js       # Vite configuration
└── README.md
```

## Key Components

### Authentication
- `Login.jsx` - User login form
- `Register.jsx` - User registration form
- `AuthContext.jsx` - Authentication state management
- `PrivateRoute.jsx` - Protected route wrapper

### Locations
- `LocationList.jsx` - Grid view of locations with filters
- `LocationDetail.jsx` - Detailed location view with ratings and comments
- `LocationForm.jsx` - Create/edit location form with image upload
- `LocationCard.jsx` - Reusable location card component

### Ratings & Reviews
- `RatingForm.jsx` - Star rating input and review form
- `RatingList.jsx` - Display list of ratings

### Comments
- `CommentSection.jsx` - Nested comment threads with replies and likes

### Images
- `ImageGallery.jsx` - Image gallery with lightbox viewer

## API Integration

All API calls go through service layers in `src/services/`:

- **authService** - Register, login, logout
- **locationService** - CRUD operations, search, filters
- **ratingService** - Create/read/delete ratings
- **commentService** - CRUD operations, replies, likes
- **imageService** - Upload/read/delete images

### Authentication Flow

1. User logs in → JWT token stored in localStorage
2. Axios interceptor adds token to all requests
3. On 401 response → redirect to login
4. AuthContext manages user state globally

## Features by Route

| Route | Description | Auth Required |
|-------|-------------|---------------|
| `/` | Home page with trending and top-rated locations | No |
| `/login` | User login | No |
| `/register` | User registration | No |
| `/locations` | Browse all locations | No |
| `/locations/:id` | View location details | No |
| `/locations/new` | Create new location | Yes |
| `/locations/:id/edit` | Edit location | Yes (owner) |
| `/profile` | User profile with locations and reviews | Yes |
| `/search?q=...` | Search locations | No |

## Styling

- **CSS Modules** - Component-scoped styles
- **Responsive Design** - Mobile-first approach
- **Color Palette**:
  - Primary: #2563eb (Blue)
  - Success: #10b981 (Green)
  - Error: #ef4444 (Red)
  - Background: #f9fafb (Light Gray)

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Development Tips

1. **Hot Module Replacement** - Changes reflect instantly without page reload
2. **React DevTools** - Install browser extension for debugging
3. **Network Tab** - Monitor API calls in browser DevTools
4. **Toast Notifications** - All API errors show user-friendly messages

## Common Issues

**API Connection Error:**
- Ensure backend is running on `http://localhost:8080`
- Check CORS configuration in Spring Boot
- Verify `VITE_API_BASE_URL` in `.env`

**Authentication Issues:**
- Clear localStorage: `localStorage.clear()`
- Check JWT token validity
- Ensure backend security is configured correctly

**Image Upload Fails:**
- Check file size limits
- Verify multipart/form-data support in backend
- Ensure uploads directory exists and is writable

## Scripts

```bash
npm run dev      # Start development server
npm run build    # Build for production
npm run preview  # Preview production build
npm run lint     # Run ESLint
```

## Contributing

1. Create feature branch
2. Make changes
3. Test thoroughly
4. Submit pull request

## License

This project is part of the Location Rating Service learning project.
