# 🎨 Frontend Verification Integration Guide

## Files Created

```
frontend/room8/src/
├── components/shared/
│   ├── VerificationBanner.jsx           # Banner for dashboards
│   └── VerificationRequiredModal.jsx    # Modal when action blocked
└── utils/
    └── verificationUtils.js             # Helper functions
```

---

## Step 1: Add Verification Banner to Dashboards

### Student Dashboard

```jsx
// In StudentDashboard.jsx
import VerificationBanner from '../../components/shared/VerificationBanner';
import { useSelector } from 'react-redux';

function StudentDashboard() {
  const { user } = useSelector(state => state.auth);

  return (
    <div>
      {/* Add banner at the top */}
      <VerificationBanner user={user} userRole="STUDENT" />
      
      {/* Rest of your dashboard */}
      <YourDashboardContent />
    </div>
  );
}
```

### Landlord Dashboard

```jsx
// In LandlordDashboard.jsx
import VerificationBanner from '../../../components/shared/VerificationBanner';
import { useSelector } from 'react-redux';

function LandlordDashboard() {
  const { user } = useSelector(state => state.auth);

  return (
    <div>
      {/* Add banner at the top */}
      <VerificationBanner user={user} userRole="LANDLORD" />
      
      {/* Rest of your dashboard */}
      <YourDashboardContent />
    </div>
  );
}
```

---

## Step 2: Handle API Errors Globally

### Update Your API Service

```javascript
// In src/services/api.js or axiosConfig.js
import axios from 'axios';
import { isVerificationRequiredError } from '../utils/verificationUtils';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
});

// Response interceptor
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Check if it's a verification error
    if (isVerificationRequiredError(error)) {
      // Dispatch event to show modal
      window.dispatchEvent(new CustomEvent('verification-required', {
        detail: {
          message: error.response.data.message,
          userRole: getCurrentUserRole() // You need to implement this
        }
      }));
    }
    
    return Promise.reject(error);
  }
);

export default api;
```

### Add Global Modal Listener

```jsx
// In App.jsx or a layout component
import { useState, useEffect } from 'react';
import VerificationRequiredModal from './components/shared/VerificationRequiredModal';
import { useSelector } from 'react-redux';

function App() {
  const [verificationModal, setVerificationModal] = useState({
    open: false,
    message: '',
    userRole: ''
  });
  
  const { user } = useSelector(state => state.auth);

  useEffect(() => {
    const handleVerificationRequired = (event) => {
      setVerificationModal({
        open: true,
        message: event.detail.message,
        userRole: event.detail.userRole || user?.role
      });
    };

    window.addEventListener('verification-required', handleVerificationRequired);
    
    return () => {
      window.removeEventListener('verification-required', handleVerificationRequired);
    };
  }, [user]);

  return (
    <>
      <YourRoutes />
      
      <VerificationRequiredModal
        open={verificationModal.open}
        onClose={() => setVerificationModal({ ...verificationModal, open: false })}
        message={verificationModal.message}
        userRole={verificationModal.userRole}
      />
    </>
  );
}
```

---

## Step 3: Disable Buttons for Unverified Users

### Listing Card Example

```jsx
// In ListingCard.jsx
import { Button, Tooltip } from '@mui/material';
import { canPerformAction, isUserVerified } from '../../utils/verificationUtils';
import { useSelector } from 'react-redux';

function ListingCard({ listing }) {
  const { user } = useSelector(state => state.auth);
  const verified = isUserVerified(user);
  const { allowed, reason } = canPerformAction(user, 'apply');

  const handleApply = () => {
    if (!allowed) {
      // Show verification modal
      window.dispatchEvent(new CustomEvent('verification-required', {
        detail: {
          message: reason,
          userRole: user?.role
        }
      }));
      return;
    }
    
    // Proceed with application
    applyToListing(listing.id);
  };

  return (
    <Card>
      <CardContent>
        {/* Listing details */}
      </CardContent>
      
      <CardActions>
        <Tooltip 
          title={!allowed ? reason : ''}
          arrow
        >
          <span> {/* Wrapper needed for disabled button tooltip */}
            <Button
              variant="contained"
              onClick={handleApply}
              disabled={!allowed}
              fullWidth
            >
              {verified ? 'Apply Now' : 'Verify to Apply'}
            </Button>
          </span>
        </Tooltip>
        
        {/* Favorite button */}
        <Tooltip 
          title={!verified ? 'Verify to save favorites' : ''}
          arrow
        >
          <span>
            <IconButton
              onClick={handleFavorite}
              disabled={!verified}
            >
              <Favorite />
            </IconButton>
          </span>
        </Tooltip>
      </CardActions>
    </Card>
  );
}
```

---

## Step 4: Create Listing Button (Landlord)

```jsx
// In LandlordDashboard.jsx or CreateListingPage.jsx
import { Button } from '@mui/material';
import { Add } from '@mui/icons-material';
import { canPerformAction } from '../../utils/verificationUtils';
import { useSelector, useNavigate } from 'react-redux';

function CreateListingButton() {
  const { user } = useSelector(state => state.auth);
  const navigate = useNavigate();
  const { allowed, reason } = canPerformAction(user, 'post');

  const handleClick = () => {
    if (!allowed) {
      window.dispatchEvent(new CustomEvent('verification-required', {
        detail: {
          message: reason,
          userRole: 'LANDLORD'
        }
      }));
      return;
    }
    
    navigate('/landlord/create-listing');
  };

  return (
    <Button
      variant="contained"
      startIcon={<Add />}
      onClick={handleClick}
      disabled={!allowed}
    >
      {allowed ? 'Create Listing' : 'Verify to Post'}
    </Button>
  );
}
```

---

## Step 5: Show Verification Status Badge

```jsx
// In UserProfile.jsx or Header.jsx
import { Chip } from '@mui/material';
import { VerifiedUser, HourglassEmpty, Cancel } from '@mui/icons-material';
import { 
  getVerificationBadgeColor, 
  getVerificationStatusText 
} from '../../utils/verificationUtils';

function VerificationBadge({ user }) {
  const status = user?.verificationStatus || 'NOT_VERIFIED';
  const color = getVerificationBadgeColor(status);
  const text = getVerificationStatusText(status);

  const getIcon = () => {
    switch (status) {
      case 'VERIFIED':
        return <VerifiedUser />;
      case 'PENDING':
        return <HourglassEmpty />;
      case 'REJECTED':
        return <Cancel />;
      default:
        return null;
    }
  };

  return (
    <Chip
      icon={getIcon()}
      label={text}
      color={color}
      size="small"
      variant={status === 'VERIFIED' ? 'filled' : 'outlined'}
    />
  );
}
```

---

## Step 6: Update Redux Store (If Needed)

```javascript
// In authSlice.js or userSlice.js
const authSlice = createSlice({
  name: 'auth',
  initialState: {
    user: null,
    token: null,
    isAuthenticated: false
  },
  reducers: {
    setUser: (state, action) => {
      state.user = {
        ...action.payload,
        // Ensure verification status is included
        verificationStatus: action.payload.verificationStatus || 'NOT_VERIFIED',
        isVerified: action.payload.isVerified || false
      };
    },
    updateVerificationStatus: (state, action) => {
      if (state.user) {
        state.user.verificationStatus = action.payload.status;
        state.user.isVerified = action.payload.status === 'VERIFIED';
      }
    }
  }
});
```

---

## Step 7: Testing Checklist

### Test as Unverified Student
- [ ] See verification banner on dashboard
- [ ] "Apply" button is disabled on listing cards
- [ ] Clicking "Apply" shows verification modal
- [ ] "Favorite" button is disabled
- [ ] Modal has "Verify Now" button that navigates to verification page

### Test as Unverified Landlord
- [ ] See verification banner on dashboard
- [ ] "Create Listing" button is disabled
- [ ] Clicking "Create Listing" shows verification modal
- [ ] Modal explains why verification is needed

### Test as Verified User
- [ ] No verification banner shown
- [ ] All buttons are enabled
- [ ] Can perform all actions
- [ ] See "Verified" badge in profile

### Test Verification Flow
- [ ] Submit verification
- [ ] Status changes to "Pending"
- [ ] Banner shows "Pending Review" message
- [ ] After admin approval, status changes to "Verified"
- [ ] Banner disappears
- [ ] All features unlock

---

## Quick Integration Summary

1. **Add to dashboards:**
   ```jsx
   <VerificationBanner user={user} userRole="STUDENT" />
   ```

2. **Add global modal to App.jsx:**
   ```jsx
   <VerificationRequiredModal ... />
   ```

3. **Disable buttons:**
   ```jsx
   const { allowed } = canPerformAction(user, 'apply');
   <Button disabled={!allowed}>Apply</Button>
   ```

4. **Handle API errors:**
   ```javascript
   if (isVerificationRequiredError(error)) {
     // Show modal
   }
   ```

---

## Next Steps

1. **Restart frontend dev server:**
   ```bash
   cd frontend/room8
   npm run dev
   ```

2. **Test the flow:**
   - Login as unverified student
   - Try to apply to a listing
   - See verification modal
   - Submit verification
   - Test as verified user

3. **Customize styling** to match your design system

4. **Add analytics** to track verification conversion rates
