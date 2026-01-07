# 💬 How to Test the Messaging System

## 🎯 **Testing Flow:**

### **Step 1: Set Up Two Users**
You need at least 2 student accounts to test messaging.

#### **Option A: Use Existing Accounts**
- Login as Student 1
- Login as Student 2 (use incognito/another browser)

#### **Option B: Create Test Accounts**
1. Go to `/signup`
2. Create Student Account 1:
   - Email: student1@test.com
   - Password: Test123!
   - Role: STUDENT

3. Create Student Account 2:
   - Email: student2@test.com
   - Password: Test123!
   - Role: STUDENT

---

### **Step 2: Set Up Preferences**
Both students need preferences to get matched.

1. Login as **Student 1**
2. Go to "Dashboard" → Set up preferences
   - Budget: 50000-100000 XAF
   - Location: Douala
   - Cleanliness: 4/5
   - Lifestyle preferences

3. Login as **Student 2** (different browser/incognito)
4. Set similar preferences
   - Budget: 50000-100000 XAF
   - Location: Douala
   - Similar lifestyle

---

### **Step 3: Find Matches**

1. As **Student 1**, go to "My Matches"
2. Click "Find New Matches" button
3. You should see Student 2 as a match

4. As **Student 2**, go to "My Matches"
5. Click "Find New Matches"
6. You should see Student 1 as a match

---

### **Step 4: Accept Each Other (Create Mutual Match)**

#### **As Student 1:**
1. See Student 2's match card
2. Click "❤️ Like" button
3. Status: "Waiting for Student 2 to respond..."

#### **As Student 2:**
1. See Student 1's match card
2. Click "❤️ Like" button
3. Status changes to: "🎉 It's a Match!"
4. **"Start Chatting"** button appears

---

### **Step 5: Start Messaging! 💬**

#### **Option 1: From Match Card**
1. Click **"Start Chatting"** button on the match card
2. Automatically redirected to Messages page
3. Conversation opens with that user

#### **Option 2: From Messages Page**
1. Go to "Messages" in sidebar
2. You'll see the matched user in conversations list
3. Click on their name
4. Chat window opens

---

### **Step 6: Test Messaging Features**

#### **Send Messages:**
1. Type a message in the input box
2. Press Enter or click send button (✈️)
3. Message appears instantly

#### **Real-Time Updates:**
- Open Messages page on both browsers
- Send message from Student 1
- See it appear instantly on Student 2's screen
- No page refresh needed! 🔥

#### **Typing Indicator:**
1. Start typing as Student 1
2. Student 2 sees "Student 1 is typing..." with animated dots
3. Stops after 3 seconds of no typing

#### **Read Receipts:**
1. Student 1 sends message (grey checkmark ✓)
2. Student 2 opens chat
3. Checkmark turns blue (✓✓)
4. Student 1 sees message was read

#### **Share Listing (if you have listings):**
1. Create a property listing as a landlord
2. Click attachment button in chat
3. Share listing link
4. Listing card appears in chat with photo, price, etc.

---

## 🔍 **What to Look For:**

### **✅ Working Correctly:**
- [ ] Messages send instantly
- [ ] "Connecting to real-time messaging..." disappears when connected
- [ ] Typing indicator shows when other person types
- [ ] Read receipts (✓ → ✓✓) work
- [ ] Conversations list updates in real-time
- [ ] Unread count badge appears
- [ ] Message timestamps show correctly
- [ ] Profile photos display
- [ ] Mobile responsive layout works

### **❌ Common Issues:**

#### **"No conversations yet"**
**Cause:** No mutual matches yet  
**Fix:** Follow Steps 1-4 to create a mutual match

#### **"Connecting to real-time messaging..."**
**Cause:** WebSocket not connecting  
**Fix:** 
- Check backend is running
- Check port 8080 is open
- Check browser console for errors

#### **Messages not sending**
**Cause:** Backend not running or API error  
**Fix:**
- Restart backend
- Check console logs
- Verify token is valid

---

## 🎮 **Quick Test Commands:**

### **Backend:**
```bash
cd backend
./mvnw spring-boot:run
```

### **Frontend:**
```bash
cd frontend/room8
npm run dev
```

### **Check WebSocket:**
Open browser console and look for:
```
WebSocket connected
Subscribed to /queue/messages/{userId}
```

---

## 📊 **Test Scenarios:**

### **Scenario 1: First Message**
1. Create mutual match
2. Click "Start Chatting"
3. Send first message
4. ✅ Conversation appears in sidebar

### **Scenario 2: Real-Time**
1. Open Messages on 2 browsers
2. Send message from Browser 1
3. ✅ Instantly appears on Browser 2
4. ✅ Typing indicator works

### **Scenario 3: Unread Count**
1. Browser 1 sends message
2. Browser 2 doesn't open chat
3. ✅ Red badge shows "1" unread
4. Browser 2 opens chat
5. ✅ Badge disappears

### **Scenario 4: Read Receipts**
1. Send message (grey ✓)
2. Other user opens chat
3. ✅ Checkmark turns blue (✓✓)

---

## 🐛 **Debugging:**

### **Check Backend Logs:**
```bash
# Look for:
INFO: WebSocket connected
INFO: Sending message from {userId} to {userId}
INFO: Message sent successfully
```

### **Check Frontend Console:**
```javascript
// Look for:
WebSocket connected
New message received: {...}
Typing indicator: {...}
Read receipt: {...}
```

### **Check Network:**
- Open DevTools → Network
- Filter: WS (WebSocket)
- Should see active connection to `ws://localhost:8080/ws`

---

## 🎉 **Success Criteria:**

You'll know it's working when:
1. ✅ "Start Chatting" button appears on mutual matches
2. ✅ Clicking it opens Messages page with that user
3. ✅ Messages send and receive in real-time
4. ✅ Typing indicator appears
5. ✅ Read receipts change from ✓ to ✓✓
6. ✅ Unread count badge shows correctly
7. ✅ No page refresh needed for updates

---

## 💡 **Pro Tips:**

1. **Use 2 different browsers** (Chrome + Firefox) for easiest testing
2. **Keep DevTools open** to see real-time logs
3. **Test mobile view** - it's fully responsive!
4. **Try all features** - typing, read receipts, delete, etc.

---

## 🚀 **What's Working:**

- ✅ Real-time messaging (WebSocket)
- ✅ Beautiful chat UI
- ✅ Typing indicators
- ✅ Read receipts
- ✅ Conversation list
- ✅ Unread counts
- ✅ Share listings (if listing exists)
- ✅ Search messages
- ✅ Delete messages
- ✅ Mobile responsive
- ✅ Automatic reconnection

---

**Happy Testing! 🎊**

*If you encounter any issues, check the browser console and backend logs for detailed error messages.*
