# MinimizURL API Integration Guide

**Version:** 1.0  
**Last Updated:** January 7, 2026  
**Backend Base URL:** `http://localhost:8081` (configurable via `VITE_API_URL` env variable)

---

## Table of Contents

1. [Authentication](#authentication)
2. [API Endpoints](#api-endpoints)
   - [Auth Endpoints](#auth-endpoints)
   - [User Endpoints](#user-endpoints)
   - [URL Shortening Endpoints](#url-shortening-endpoints)
3. [Response Formats](#response-formats)
4. [Error Handling](#error-handling)
5. [Important Notes](#important-notes)

---

## Authentication

### JWT Token

All authenticated requests must include the JWT token in the `Authorization` header:

```
Authorization: Bearer <token>
```

### Token Lifecycle

- Token is returned upon successful login or registration
- For OAuth2 (Google), token is passed as a query parameter to `/api/auth/auth-success?token=<token>`
- On 401 response, clear local storage and redirect to login

---

## API Endpoints

### Auth Endpoints

#### `POST /api/auth/register`

Register a new user account.

**Request Body (JSON):**
```json
{
  "username": "johndoe",
  "password": "securePassword123",
  "email": "john@example.com"
}
```

**Response:** `200 OK`
```
"User registered successfully!"
```

**Validation:**
- `email` is required and must be valid format
- `username` and `password` are required

---

#### `POST /api/auth/login`

Authenticate user and receive JWT token.

**Request Body (JSON):**
```json
{
  "username": "johndoe",
  "password": "securePassword123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

#### `GET /oauth2/authorization/google`

**⚠️ This is NOT an API call - redirect the user's browser to this URL**

Initiates Google OAuth2 flow. After successful authentication, user is redirected to your callback URL with a token parameter.

**Usage:**
```javascript
window.location.href = `${API_BASE_URL}/oauth2/authorization/google`
```

---

#### `GET /api/auth/auth-success`

Called after OAuth2 redirect to retrieve the JWT token.

**Query Parameters:**
| Parameter | Type   | Required | Description           |
|-----------|--------|----------|-----------------------|
| `token`   | string | Yes      | Token from OAuth redirect |

**Response:** `200 OK`
```json
{
  "message": "Google Authentication Successful!",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer"
}
```

---

#### `POST /api/auth/forgot-password`

Request a password reset email.

**⚠️ IMPORTANT:** Email is passed as a **query parameter**, NOT in the request body.

**Query Parameters:**
| Parameter | Type   | Required | Description        |
|-----------|--------|----------|--------------------|
| `email`   | string | Yes      | User's email address |

**Example:**
```
POST /api/auth/forgot-password?email=john@example.com
```

**Response:** `200 OK`
```
"If an account is associated with this email and is a local account, a reset link has been sent."
```

**Note:** Response is always the same for security (prevents email enumeration).

---

#### `POST /api/auth/reset-password`

Reset password using the token from email.

**Request Body (JSON):**
```json
{
  "token": "reset-token-from-email",
  "newPassword": "myNewSecurePassword123"
}
```

**⚠️ IMPORTANT:** The field is `newPassword`, NOT `password`.

**Response:** `200 OK`
```
"Password has been successfully reset."
```

---

#### `DELETE /api/auth/delete-account`

Delete the currently authenticated user's account and all associated data.

**🔒 Requires Authentication**

**Response:** `200 OK`
```
"Account and all associated data deleted successfully."
```

---

### User Endpoints

#### `GET /api/user/me`

Get the current user's profile information.

**🔒 Requires Authentication**

**Response:** `200 OK`
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "provider": "local",
  "totalLinksCreated": 15
}
```

**Provider Values:**
- `"local"` - Registered via email/password
- `"google"` - Registered via Google OAuth

---

### URL Shortening Endpoints

#### `POST /mini/shorten`

Create a shortened URL.

**⚠️ IMPORTANT:** Parameters are passed as **query parameters**, NOT in the request body.

**Query Parameters:**
| Parameter    | Type   | Required | Description                |
|--------------|--------|----------|----------------------------|
| `url`        | string | Yes      | Original URL to shorten    |
| `customCode` | string | No       | Custom short code (optional) |

**Example:**
```
POST /mini/shorten?url=https://example.com/very-long-url
POST /mini/shorten?url=https://example.com&customCode=mylink
```

**Response:** `200 OK`
```
"abc123"
```
(Returns the short code as plain text)

**Error Response:** `409 Conflict`
```
"Error: Custom code is already in use."
```

**Notes:**
- Works for both authenticated and guest users
- Guest users' links are tracked with userId "GUEST"
- Links expire after 30 days of inactivity (reset on each click)

---

#### `GET /mini/{shortCode}`

**⚠️ This is a REDIRECT endpoint, not an API call**

Redirects to the original URL. Used when users visit shortened links.

**Response:** `302 Redirect` to original URL

---

#### `GET /mini/stats/{shortCode}`

Get basic statistics for a shortened URL.

**Response:** `200 OK`
```json
{
  "id": 12345,
  "originalUrl": "https://example.com/original",
  "clicks": 42,
  "expirationDate": "2026-02-06T10:30:00Z",
  "createdDate": "2026-01-07T10:30:00Z",
  "customCode": "mylink",
  "userId": "507f1f77bcf86cd799439011"
}
```

---

#### `DELETE /mini/{shortCode}`

Delete a shortened URL.

**🔒 Requires Authentication + Ownership**

**Response:** `204 No Content`

---

#### `PUT /mini/{shortCode}`

Update the destination URL of a shortened link.

**🔒 Requires Authentication + Ownership**

**Request Body (JSON):**
```json
{
  "newUrl": "https://new-destination.com"
}
```

**Response:** `200 OK`
```json
{
  "id": 12345,
  "originalUrl": "https://new-destination.com",
  "clicks": 42,
  "expirationDate": "2026-02-06T10:30:00Z",
  "createdDate": "2026-01-07T10:30:00Z",
  "customCode": "mylink",
  "userId": "507f1f77bcf86cd799439011"
}
```

---

#### `GET /mini/{shortCode}/fstats`

Get full analytics for a shortened URL.

**🔒 Requires Authentication + Ownership**

**Response:** `200 OK`
```json
{
  "summary": {
    "totalClicks": 1250,
    "lastClick": "2026-01-07T14:30:00Z"
  },
  "topReferrers": [
    { "_id": "Direct", "count": 500 },
    { "_id": "https://twitter.com", "count": 300 },
    { "_id": "https://facebook.com", "count": 200 }
  ],
  "deviceBreakdown": [
    { "_id": "Desktop", "count": 800 },
    { "_id": "Mobile", "count": 450 }
  ],
  "dailyTrend": [
    { "_id": "2026-01-01", "count": 150 },
    { "_id": "2026-01-02", "count": 180 },
    { "_id": "2026-01-03", "count": 220 }
  ]
}
```

---

## Response Formats

### Success Responses

| Status Code | Meaning                    |
|-------------|----------------------------|
| `200`       | Success with response body |
| `204`       | Success, no content        |
| `302`       | Redirect                   |

### Error Responses

| Status Code | Meaning                          |
|-------------|----------------------------------|
| `400`       | Bad Request (validation error)   |
| `401`       | Unauthorized (no/invalid token)  |
| `403`       | Forbidden (not owner of resource)|
| `404`       | Resource not found               |
| `409`       | Conflict (duplicate resource)    |
| `500`       | Internal server error            |

---

## Error Handling

### Standard Error Response Format

```json
{
  "timestamp": "2026-01-07T10:30:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Link not found: abc123",
  "path": "/mini/abc123"
}
```

### Common Exceptions

| Exception                  | HTTP Status | Description                          |
|----------------------------|-------------|--------------------------------------|
| `LinkNotFoundException`    | 404         | Short code doesn't exist             |
| `UnauthorizedAccessException` | 403      | User doesn't own the resource        |
| `DuplicateUsernameException` | 409       | Username already taken               |
| `InvalidLoginException`    | 401         | Wrong username/password              |
| `InvalidTokenException`    | 400         | Reset token expired or invalid       |

---

## Important Notes

### ⚠️ Critical Differences from Common Patterns

1. **`/mini/shorten` uses Query Parameters**
   ```javascript
   // ❌ WRONG
   api.post('/mini/shorten', { url: 'https://example.com' })
   
   // ✅ CORRECT
   api.post('/mini/shorten', null, { params: { url: 'https://example.com' } })
   ```

2. **`/api/auth/forgot-password` uses Query Parameters**
   ```javascript
   // ❌ WRONG
   api.post('/api/auth/forgot-password', { email: 'user@example.com' })
   
   // ✅ CORRECT
   api.post('/api/auth/forgot-password', null, { params: { email: 'user@example.com' } })
   ```

3. **Reset Password uses `newPassword`, not `password`**
   ```javascript
   // ❌ WRONG
   api.post('/api/auth/reset-password', { token, password: 'newPass' })
   
   // ✅ CORRECT
   api.post('/api/auth/reset-password', { token, newPassword: 'newPass' })
   ```

4. **Analytics endpoint is `/fstats`, not `/analytics`**
   ```javascript
   // ❌ WRONG
   api.get(`/mini/${shortCode}/analytics`)
   
   // ✅ CORRECT
   api.get(`/mini/${shortCode}/fstats`)
   ```

5. **Delete account is under `/api/auth`, not `/api/user`**
   ```javascript
   // ❌ WRONG
   api.delete('/api/user/me')
   
   // ✅ CORRECT
   api.delete('/api/auth/delete-account')
   ```

6. **No `/mini/my-links` endpoint exists**
   - To get user's links, you need to track them client-side or request a new backend endpoint

---

## Missing Endpoints (Backend Enhancement Needed)

The following endpoints may be needed for a complete frontend:

| Endpoint | Purpose | Status |
|----------|---------|--------|
| `GET /mini/my-links` | Get all links created by authenticated user | ❌ Not implemented |
| `PUT /api/user/me` | Update user profile | ❌ Not implemented |

---

## CORS Configuration

The backend should be configured to accept requests from your frontend domain. Current security config allows:
- Public endpoints: `/mini/shorten`, `/mini/{shortCode}`, `/api/auth/**`
- Authenticated endpoints: `/api/user/**`, `/mini/{shortCode}/fstats`, etc.

---

## Example: Complete Login Flow

```javascript
// 1. Login
const response = await authAPI.login({ 
  username: 'johndoe', 
  password: 'secret123' 
})

// 2. Store token
localStorage.setItem('token', response.data.token)

// 3. Subsequent API calls automatically include token
const profile = await userAPI.getMe()
console.log(profile.data) // { username, email, ... }
```

## Example: OAuth2 Flow

```javascript
// 1. Redirect user to Google
window.location.href = authAPI.googleAuthUrl()

// 2. After redirect back to your app (e.g., /auth/callback?token=xxx)
const urlParams = new URLSearchParams(window.location.search)
const token = urlParams.get('token')

// 3. Verify and store
const response = await authAPI.authSuccess(token)
localStorage.setItem('token', response.data.token)
```

---

## Contact

For backend issues or endpoint additions, contact the backend team.

