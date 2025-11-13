# API Documentation

Complete REST API documentation for the Location Rating Service.

## Base URL

```
Local: http://localhost:8080
Docker: http://localhost:8080
```

## Authentication

Most endpoints require JWT authentication. Include the token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

---

## Table of Contents

1. [Authentication Endpoints](#authentication-endpoints)
2. [Location Endpoints](#location-endpoints)
3. [Rating Endpoints](#rating-endpoints)
4. [Comment Endpoints](#comment-endpoints)
5. [Image Endpoints](#image-endpoints)
6. [Error Responses](#error-responses)
7. [Mobile Integration Guide](#mobile-integration-guide)

---

## Authentication Endpoints

### Register User

Creates a new user account.

**Endpoint**: `POST /api/auth/register`

**Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "Password123",
  "fullName": "John Doe"
}
```

**Validation Rules**:
- `username`: 3-50 characters, alphanumeric with underscores/hyphens
- `email`: Valid email format
- `password`: Min 8 characters, must contain uppercase, lowercase, and digit
- `fullName`: Optional, max 100 characters

**Success Response** (201 Created):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "role": "USER"
}
```

**Error Responses**:
- `400 Bad Request`: Validation errors or email/username already exists
- `500 Internal Server Error`: Server error

---

### Login

Authenticates a user and returns JWT token.

**Endpoint**: `POST /api/auth/login`

**Request Body**:
```json
{
  "email": "john@example.com",
  "password": "Password123"
}
```

**Success Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "role": "USER"
}
```

**Error Responses**:
- `401 Unauthorized`: Invalid credentials
- `400 Bad Request`: Validation errors

---

## Location Endpoints

### Create Location

Creates a new location with geolocation data.

**Endpoint**: `POST /api/locations`

**Authentication**: Required

**Request Body**:
```json
{
  "name": "Central Park",
  "description": "Beautiful park in the heart of Manhattan",
  "latitude": 40.785091,
  "longitude": -73.968285,
  "address": "Central Park",
  "city": "New York",
  "country": "USA",
  "postalCode": "10024",
  "category": "PARK"
}
```

**Categories**: `RESTAURANT`, `CAFE`, `HOTEL`, `ATTRACTION`, `PARK`, `MUSEUM`, `SHOPPING`, `ENTERTAINMENT`, `EDUCATION`, `HEALTHCARE`, `TRANSPORTATION`, `SPORTS`, `OTHER`

**Success Response** (201 Created):
```json
{
  "id": 1,
  "name": "Central Park",
  "description": "Beautiful park in the heart of Manhattan",
  "latitude": 40.785091,
  "longitude": -73.968285,
  "address": "Central Park",
  "city": "New York",
  "country": "USA",
  "postalCode": "10024",
  "category": "PARK",
  "averageRating": 0.0,
  "totalRatings": 0,
  "viewCount": 0,
  "active": true,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00",
  "userId": 1,
  "username": "johndoe",
  "images": []
}
```

---

### Get Location by ID

Retrieves details of a specific location.

**Endpoint**: `GET /api/locations/{id}`

**Authentication**: Not required

**Success Response** (200 OK):
```json
{
  "id": 1,
  "name": "Central Park",
  "description": "Beautiful park in the heart of Manhattan",
  "latitude": 40.785091,
  "longitude": -73.968285,
  "address": "Central Park",
  "city": "New York",
  "country": "USA",
  "averageRating": 4.5,
  "totalRatings": 120,
  "viewCount": 1523,
  "active": true,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00",
  "userId": 1,
  "username": "johndoe",
  "images": [
    {
      "id": 1,
      "url": "/api/images/view/abc123.jpg",
      "caption": "Beautiful sunset view",
      "isPrimary": true
    }
  ]
}
```

---

### Find Nearby Locations

Finds locations within a specified radius using geolocation.

**Endpoint**: `GET /api/locations/nearby`

**Query Parameters**:
- `latitude` (required): Latitude coordinate
- `longitude` (required): Longitude coordinate
- `radiusKm` (optional): Search radius in kilometers (default: 10.0)
- `limit` (optional): Max results (default: 50)

**Example**:
```
GET /api/locations/nearby?latitude=40.785091&longitude=-73.968285&radiusKm=5&limit=20
```

**Success Response** (200 OK):
```json
[
  {
    "id": 1,
    "name": "Central Park",
    "latitude": 40.785091,
    "longitude": -73.968285,
    "averageRating": 4.5,
    "totalRatings": 120,
    "distanceKm": 0.5,
    ...
  },
  {
    "id": 2,
    "name": "Times Square",
    "latitude": 40.758896,
    "longitude": -73.985130,
    "averageRating": 4.2,
    "totalRatings": 89,
    "distanceKm": 3.2,
    ...
  }
]
```

---

### Search Locations

Searches locations by name keyword.

**Endpoint**: `GET /api/locations/search`

**Query Parameters**:
- `keyword` (required): Search keyword

**Example**:
```
GET /api/locations/search?keyword=park
```

---

### Get All Locations

Retrieves paginated list of all active locations.

**Endpoint**: `GET /api/locations`

**Query Parameters**:
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)

**Example**:
```
GET /api/locations?page=0&size=10
```

**Success Response** (200 OK):
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 150,
  "totalPages": 15,
  "last": false
}
```

---

### Update Location

Updates an existing location (owner only).

**Endpoint**: `PUT /api/locations/{id}`

**Authentication**: Required (must be owner)

**Request Body**: Same as Create Location

**Success Response** (200 OK): Location object

---

### Delete Location

Soft deletes a location (owner only).

**Endpoint**: `DELETE /api/locations/{id}`

**Authentication**: Required (must be owner)

**Success Response** (204 No Content)

---

### Get Top-Rated Locations

Retrieves highest-rated locations.

**Endpoint**: `GET /api/locations/top-rated`

**Query Parameters**:
- `limit` (optional): Number of results (default: 10)

---

### Get Trending Locations

Retrieves most viewed locations recently.

**Endpoint**: `GET /api/locations/trending`

**Query Parameters**:
- `limit` (optional): Number of results (default: 10)

---

## Rating Endpoints

### Add or Update Rating

Creates a new rating or updates existing rating for a location.

**Endpoint**: `POST /api/ratings`

**Authentication**: Required

**Request Body**:
```json
{
  "locationId": 1,
  "score": 4.5,
  "review": "Amazing place! Highly recommended."
}
```

**Validation**:
- `score`: 1.0 to 5.0
- `review`: Optional, max 1000 characters

**Success Response** (201 Created):
```json
{
  "id": 1,
  "locationId": 1,
  "locationName": "Central Park",
  "userId": 1,
  "username": "johndoe",
  "score": 4.5,
  "review": "Amazing place! Highly recommended.",
  "createdAt": "2024-01-15T11:00:00",
  "updatedAt": "2024-01-15T11:00:00"
}
```

---

### Get Ratings for Location

Retrieves all ratings for a specific location.

**Endpoint**: `GET /api/ratings/location/{locationId}`

**Success Response** (200 OK):
```json
[
  {
    "id": 1,
    "locationId": 1,
    "locationName": "Central Park",
    "userId": 1,
    "username": "johndoe",
    "score": 4.5,
    "review": "Amazing place!",
    "createdAt": "2024-01-15T11:00:00"
  }
]
```

---

### Get Rating Distribution

Gets rating distribution (1-5 stars) for a location.

**Endpoint**: `GET /api/ratings/location/{locationId}/distribution`

**Success Response** (200 OK):
```json
[
  [5.0, 45],  // 45 five-star ratings
  [4.0, 30],  // 30 four-star ratings
  [3.0, 20],
  [2.0, 3],
  [1.0, 2]
]
```

---

### Delete Rating

Deletes a rating (owner only).

**Endpoint**: `DELETE /api/ratings/{id}`

**Authentication**: Required (must be owner)

**Success Response** (204 No Content)

---

## Comment Endpoints

### Add Comment or Reply

Creates a new comment or reply to an existing comment.

**Endpoint**: `POST /api/comments`

**Authentication**: Required

**Request Body**:

For top-level comment:
```json
{
  "locationId": 1,
  "content": "This place is amazing!"
}
```

For reply:
```json
{
  "locationId": 1,
  "content": "I totally agree!",
  "parentCommentId": 5
}
```

**Success Response** (201 Created):
```json
{
  "id": 1,
  "locationId": 1,
  "locationName": "Central Park",
  "userId": 1,
  "username": "johndoe",
  "userProfileImage": "/api/images/view/profile.jpg",
  "content": "This place is amazing!",
  "edited": false,
  "likeCount": 0,
  "createdAt": "2024-01-15T12:00:00",
  "updatedAt": "2024-01-15T12:00:00",
  "parentCommentId": null,
  "isReply": false,
  "replyCount": 0,
  "replies": []
}
```

---

### Get Comments for Location

Retrieves all top-level comments with nested replies.

**Endpoint**: `GET /api/comments/location/{locationId}`

**Success Response** (200 OK):
```json
[
  {
    "id": 1,
    "locationId": 1,
    "userId": 1,
    "username": "johndoe",
    "content": "This place is amazing!",
    "edited": false,
    "likeCount": 5,
    "createdAt": "2024-01-15T12:00:00",
    "isReply": false,
    "replyCount": 2,
    "replies": [
      {
        "id": 2,
        "userId": 2,
        "username": "janedoe",
        "content": "I totally agree!",
        "likeCount": 1,
        "createdAt": "2024-01-15T12:30:00",
        "isReply": true,
        "parentCommentId": 1
      }
    ]
  }
]
```

---

### Update Comment

Updates comment content (owner only).

**Endpoint**: `PUT /api/comments/{id}`

**Authentication**: Required (must be owner)

**Request Body**:
```json
{
  "content": "Updated comment text"
}
```

**Success Response** (200 OK): Comment object with `edited: true`

---

### Delete Comment

Deletes a comment (owner only).

**Endpoint**: `DELETE /api/comments/{id}`

**Authentication**: Required (must be owner)

**Success Response** (204 No Content)

---

### Like Comment

Increments like count for a comment.

**Endpoint**: `POST /api/comments/{id}/like`

**Authentication**: Required

**Success Response** (200 OK): Comment object with updated like count

---

## Image Endpoints

### Upload Image

Uploads an image for a location.

**Endpoint**: `POST /api/images/upload`

**Authentication**: Required

**Content-Type**: `multipart/form-data`

**Form Data**:
- `locationId` (required): Location ID
- `file` (required): Image file (JPEG, PNG, GIF, WebP, max 5MB)
- `caption` (optional): Image caption
- `isPrimary` (optional): Set as primary image (boolean)

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/images/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "locationId=1" \
  -F "file=@/path/to/image.jpg" \
  -F "caption=Beautiful view" \
  -F "isPrimary=true"
```

**Success Response** (201 Created):
```json
{
  "id": 1,
  "locationId": 1,
  "userId": 1,
  "username": "johndoe",
  "fileName": "abc123.jpg",
  "url": "/api/images/view/abc123.jpg",
  "contentType": "image/jpeg",
  "fileSize": 245678,
  "caption": "Beautiful view",
  "isPrimary": true,
  "displayOrder": 0,
  "createdAt": "2024-01-15T13:00:00"
}
```

---

### View Image

Returns the actual image file.

**Endpoint**: `GET /api/images/view/{filename}`

**Response**: Image file (binary)

**Example**:
```html
<img src="http://localhost:8080/api/images/view/abc123.jpg" alt="Location image">
```

---

### Get Images for Location

Retrieves all images for a location.

**Endpoint**: `GET /api/images/location/{locationId}`

**Success Response** (200 OK):
```json
[
  {
    "id": 1,
    "url": "/api/images/view/abc123.jpg",
    "caption": "Beautiful view",
    "isPrimary": true,
    "displayOrder": 0,
    "createdAt": "2024-01-15T13:00:00"
  }
]
```

---

### Delete Image

Deletes an image (owner only).

**Endpoint**: `DELETE /api/images/{id}`

**Authentication**: Required (must be owner)

**Success Response** (204 No Content)

---

## Error Responses

All error responses follow this format:

```json
{
  "status": 400,
  "message": "Error description",
  "timestamp": "2024-01-15T14:00:00"
}
```

### Validation Errors

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "email": "Invalid email format",
    "password": "Password must be at least 8 characters"
  },
  "timestamp": "2024-01-15T14:00:00"
}
```

### Common Status Codes

- `200 OK`: Successful GET request
- `201 Created`: Successful POST request (resource created)
- `204 No Content`: Successful DELETE request
- `400 Bad Request`: Validation error or invalid input
- `401 Unauthorized`: Missing or invalid authentication
- `403 Forbidden`: Authenticated but not authorized
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

---

## Mobile Integration Guide

### Android/Kotlin Integration Example

#### 1. Add Dependencies (build.gradle)

```kotlin
dependencies {
    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp for interceptors
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
}
```

#### 2. Create API Service

```kotlin
interface LocationApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @GET("api/locations/{id}")
    suspend fun getLocation(@Path("id") id: Long): LocationResponse

    @GET("api/locations/nearby")
    suspend fun getNearbyLocations(
        @Query("latitude") lat: Double,
        @Query("longitude") lng: Double,
        @Query("radiusKm") radius: Double = 10.0
    ): List<LocationResponse>

    @POST("api/locations")
    suspend fun createLocation(@Body request: LocationRequest): LocationResponse

    @Multipart
    @POST("api/images/upload")
    suspend fun uploadImage(
        @Part("locationId") locationId: RequestBody,
        @Part file: MultipartBody.Part,
        @Part("caption") caption: RequestBody?
    ): ImageResponse
}
```

#### 3. Create Retrofit Instance with JWT Interceptor

```kotlin
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider()

        val newRequest = if (token != null) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        return chain.proceed(newRequest)
    }
}

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/" // Android emulator

    val apiService: LocationApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor {
            // Get token from SharedPreferences/DataStore
            TokenManager.getToken()
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LocationApiService::class.java)
    }
}
```

#### 4. Usage in ViewModel

```kotlin
class LocationViewModel(
    private val apiService: LocationApiService
) : ViewModel() {

    private val _locations = MutableStateFlow<List<LocationResponse>>(emptyList())
    val locations: StateFlow<List<LocationResponse>> = _locations.asStateFlow()

    fun loadNearbyLocations(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val result = apiService.getNearbyLocations(lat, lng, radius = 10.0)
                _locations.value = result
            } catch (e: Exception) {
                // Handle error
                Log.e("LocationViewModel", "Error loading locations", e)
            }
        }
    }

    fun createLocation(request: LocationRequest) {
        viewModelScope.launch {
            try {
                val result = apiService.createLocation(request)
                // Handle success
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
```

#### 5. Image Upload Example

```kotlin
fun uploadImage(locationId: Long, imageUri: Uri) {
    viewModelScope.launch {
        try {
            val file = uriToFile(imageUri)
            val requestFile = file.asRequestBody("image/*".toMediaType())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val locationIdBody = locationId.toString().toRequestBody("text/plain".toMediaType())

            val result = apiService.uploadImage(locationIdBody, body, null)
            // Handle success
        } catch (e: Exception) {
            // Handle error
        }
    }
}
```

---

## Rate Limiting & Best Practices

1. **Pagination**: Always use pagination for list endpoints
2. **Caching**: Implement local caching for frequently accessed data
3. **Error Handling**: Handle all HTTP status codes appropriately
4. **Token Refresh**: Implement token refresh logic before expiration
5. **Retry Logic**: Implement exponential backoff for network errors
6. **Image Optimization**: Compress images before upload
7. **Background Sync**: Use WorkManager for background operations

---

For interactive API testing, visit the Swagger UI at:
**http://localhost:8080/swagger-ui.html**
