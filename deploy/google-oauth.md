# Google OAuth — production (P3)

The SPA starts OAuth on **www** (same origin). Next.js proxies to Railway API with `X-Forwarded-Host`.

## Google Cloud Console

1. Project: `roombay` (or your name)
2. OAuth consent screen: External, scopes `email` + `profile`
3. OAuth 2.0 Client ID — **Web application**

### Authorized redirect URIs

```
https://www.roombay.com/login/oauth2/code/google
```

If the apex domain serves the app, also add:

```
https://roombay.com/login/oauth2/code/google
```

### Authorized JavaScript origins

```
https://www.roombay.com
https://roombay.com
```

## Railway variables

```
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
APP_OAUTH_GOOGLE_ENABLED=true
APP_FRONTEND_URL=https://www.roombay.com
APP_BASE_URL=https://api.roombay.com
```

## Vercel

```
BACKEND_URL=https://api.roombay.com
NEXT_PUBLIC_OAUTH_GOOGLE_ENABLED=true
```

Testing mode: add test users until you publish the consent screen.
