# 🔍 **Pagination Debugging Guide**

## **Why You Might Not See Pagination:**

### **1. No Data in Database** ⚠️
The pagination only shows when there's data. If you don't have users in the database, you won't see pagination.

**Solution:** Create some test users first.

---

### **2. Backend Not Running** ⚠️
If the backend API is not running, no data will be fetched.

**Check:**
1. Is your Spring Boot backend running?
2. Check console for API errors
3. Open browser DevTools → Network tab → Look for failed requests

---

### **3. Frontend Dev Server Needs Refresh** ⚠️
Sometimes Vite needs a hard refresh after adding new components.

**Try:**
```bash
# Stop the dev server (Ctrl+C)
cd frontend/room8

# Clear cache and restart
rm -rf node_modules/.vite
npm run dev

# Or on Windows PowerShell:
Remove-Item -Recurse -Force node_modules/.vite
npm run dev
```

---

## **🔬 Debug Steps:**

### **Step 1: Check Browser Console**
Open DevTools (F12) and look for:

1. **Console logs showing pagination data:**
   ```
   Pagination data: { totalPages: 5, totalElements: 100, currentPage: 0 }
   ```

2. **Network requests:**
   - Look for `/api/admin/users` request
   - Check if it's returning data
   - Check the response structure

---

### **Step 2: Check If Backend Is Running**

Open: `http://localhost:8080/api/admin/users?page=0&size=20`

**Expected response:**
```json
{
  "content": [...users array...],
  "totalPages": 5,
  "totalElements": 100,
  "size": 20,
  "number": 0
}
```

**If you get an error:**
- Backend is not running → Start it
- 401 Unauthorized → You're not logged in as admin
- 403 Forbidden → User doesn't have ADMIN role

---

### **Step 3: Verify Admin Access**

1. **Login with an admin account**
2. **Check localStorage for token:**
   ```javascript
   // In browser console:
   localStorage.getItem('accessToken')
   ```

3. **Verify user is admin:**
   - The admin endpoints require `hasRole('ADMIN')`
   - Make sure your user has `role: 'ADMIN'` in database

---

### **Step 4: Add Test Users to Database**

If your database is empty, run this SQL:

```sql
-- Add some test users
INSERT INTO users (id, email, phone, password, role, first_name, last_name, email_verified, phone_verified, account_status, created_at)
VALUES 
  (gen_random_uuid(), 'student1@test.com', '1234567891', 'password', 'STUDENT', 'John', 'Doe', true, true, 'ACTIVE', NOW()),
  (gen_random_uuid(), 'student2@test.com', '1234567892', 'password', 'STUDENT', 'Jane', 'Smith', true, true, 'ACTIVE', NOW()),
  (gen_random_uuid(), 'student3@test.com', '1234567893', 'password', 'STUDENT', 'Bob', 'Johnson', true, true, 'ACTIVE', NOW()),
  (gen_random_uuid(), 'landlord1@test.com', '1234567894', 'password', 'LANDLORD', 'Alice', 'Williams', true, true, 'ACTIVE', NOW()),
  (gen_random_uuid(), 'landlord2@test.com', '1234567895', 'password', 'LANDLORD', 'Charlie', 'Brown', true, true, 'ACTIVE', NOW());
```

---

### **Step 5: Test Pagination Component Directly**

Add this to your page temporarily to test:

```jsx
{/* Debug: Force show pagination */}
<div className="p-4 bg-yellow-100 border border-yellow-300 rounded">
  <p>Debug Info:</p>
  <p>Total Pages: {totalPages}</p>
  <p>Total Elements: {totalElements}</p>
  <p>Current Page: {page}</p>
  <p>Users Count: {users.length}</p>
</div>

<Pagination
  currentPage={page}
  totalPages={totalPages}
  totalElements={totalElements}
  pageSize={20}
  onPageChange={setPage}
/>
```

---

## **🎯 Quick Checklist:**

- [ ] Backend is running (`http://localhost:8080`)
- [ ] Frontend is running (`http://localhost:5173` or `5174`)
- [ ] Logged in as an ADMIN user
- [ ] Database has users (at least 1)
- [ ] Browser console shows no errors
- [ ] Network tab shows successful API responses
- [ ] Console log shows pagination data
- [ ] Tried hard refresh (Ctrl+Shift+R)

---

## **🔧 Common Fixes:**

### **Fix 1: Hard Refresh Browser**
```
Ctrl + Shift + R (Windows/Linux)
Cmd + Shift + R (Mac)
```

### **Fix 2: Restart Frontend**
```bash
cd frontend/room8
# Kill the process (Ctrl+C)
npm run dev
```

### **Fix 3: Restart Backend**
```bash
cd backend
mvn spring-boot:run
```

### **Fix 4: Clear Browser Cache**
1. DevTools → Application → Clear storage → Clear site data
2. Or use Incognito/Private mode

---

## **📊 What You Should See:**

### **With Data:**
```
┌─────────────────────────────────────────┐
│  User Management                        │
├─────────────────────────────────────────┤
│  [Search] [Filters] [Stats]             │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │ Table with users                │   │
│  └─────────────────────────────────┘   │
│                                         │
│  Showing 1 to 20 of 100 results        │
│  [<] [1] [2] [3] [4] [5] ... [10] [>] │
└─────────────────────────────────────────┘
```

### **With No Data:**
```
┌─────────────────────────────────────────┐
│  User Management                        │
├─────────────────────────────────────────┤
│  [Search] [Filters] [Stats]             │
│                                         │
│  No users found                         │
│  (No pagination shown)                  │
└─────────────────────────────────────────┘
```

---

## **💡 Still Not Working?**

### **Share this info:**

1. **Console output:**
   - Any errors?
   - What does the pagination data log show?

2. **Network tab:**
   - Is `/api/admin/users` being called?
   - What's the response status?
   - What's the response body?

3. **Current state:**
   - How many users in database?
   - Are you logged in as admin?
   - What page are you on?

---

## **✅ Expected Behavior:**

- **0 users:** No pagination shown ✓
- **1-20 users:** Pagination shows "Showing 1 to X of X results" (no page numbers) ✓
- **21+ users:** Full pagination with page numbers ✓

---

**Check these and let me know what you find!** 🔍
