# Clestra Serverless Offline Relay Backend

This directory contains the lightweight, serverless Node.js backend API for **Clestra**.

When chatting peers are **online**, messages are delivered directly between them peer-to-peer (P2P). When a recipient is **offline**, Clestra compresses and routes the message through this serverless API, which stores it ephemerally in a free-tier **MongoDB Atlas** database.

When the recipient reconnects, Clestra calls the sync endpoint. The messages are delivered to the recipient and **immediately deleted from MongoDB Atlas**, ensuring active database storage remains near **0 MB** (well within MongoDB Atlas's 512 MB free tier).

---

## 🚀 System Architecture & API Endpoints

### 1. Offline Message Store: `POST /api/messages/send`
- **Request Body (JSON):**
  ```json
  {
    "senderId": "peer_maya_p2p",
    "recipientId": "user_clestra_me",
    "compressedContent": "eJzLSM3JyVcozy/KSVEEABx4A5s=",
    "timestamp": 1731174200000
  }
  ```
- **Action:** Inserts the message document into the `pending_messages` collection with `delivered: false`.
- **Response (201 Created):**
  ```json
  {
    "success": true,
    "messageId": "672f...",
    "status": "queued_offline_serverless"
  }
  ```

---

### 2. Message Sync & Auto-Cleanup: `GET /api/messages/sync`
- **Query Parameter:** `recipientId` (e.g. `/api/messages/sync?recipientId=user_clestra_me`)
- **Action:**
  1. Finds all documents matching `recipientId` sorted by `timestamp`.
  2. Returns the array of pending messages to the client.
  3. **Auto-Cleanup:** Immediately deletes the retrieved messages from MongoDB using `deleteMany({ _id: { $in: ids } })` so active storage stays near 0 MB.
- **Response (200 OK):**
  ```json
  {
    "success": true,
    "recipientId": "user_clestra_me",
    "count": 1,
    "messages": [
      {
        "id": "672f...",
        "senderId": "peer_maya_p2p",
        "recipientId": "user_clestra_me",
        "compressedContent": "eJzLSM3JyVcozy/KSVEEABx4A5s=",
        "timestamp": 1731174200000,
        "delivered": true
      }
    ]
  }
  ```

---

### 3. Health Check: `GET /api/health`
- **Response (200 OK):**
  ```json
  {
    "service": "Clestra Serverless Offline Relay",
    "status": "online",
    "mongodb": "connected",
    "timestamp": 1731174200000
  }
  ```

---

## 🛠️ Step-by-Step MongoDB Atlas Setup Guide

1. **Sign Up for MongoDB Atlas:**
   - Go to [mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas)
   - Create a free account and choose **Shared M0 Cluster (512 MB Free Forever)**.

2. **Create a Database User:**
   - In Atlas, go to **Database Access** -> **Add New Database User**.
   - Select **Password Authentication**.
   - Set a Username (e.g. `clestra_admin`) and secure Password.
   - Assign the **Read and write to any database** role.

3. **Configure Network Access (Whitelist IP):**
   - Go to **Network Access** -> **Add IP Address**.
   - Choose **Allow Access from Anywhere** (`0.0.0.0/0`) because serverless runtimes use dynamic IP addresses.

4. **Copy Connection String (`MONGODB_URI`):**
   - In Database -> Click **Connect** -> **Drivers** (Node.js).
   - Your connection string will look like:
     ```
     mongodb+srv://<username>:<password>@cluster0.abcde.mongodb.net/clestra_relay?retryWrites=true&w=majority
     ```
   - Replace `<username>` and `<password>` with your database user credentials.

---

## ⚡ Deployment Options (Zero-Cost Serverless)

### Option A: Deploy to Vercel (1-Click)
1. Install Vercel CLI or connect via GitHub:
   ```bash
   cd serverless
   npm install -g vercel
   vercel
   ```
2. Set Environment Variable in Vercel Dashboard (or CLI):
   ```bash
   vercel env add MONGODB_URI
   # Paste your MongoDB Atlas URI string
   ```
3. Your API will be live at `https://your-project.vercel.app`.
4. In the Clestra Android app, navigate to **Settings** -> Enter your Vercel URL -> Tap **Save & Test Connection**.

### Option B: Local Testing
Run locally with Node.js:
```bash
cd serverless
npm install
export MONGODB_URI="mongodb+srv://..."
npm start
```
The server will listen on `http://localhost:3000`. In the Android Emulator, configure the relay URL as `http://10.0.2.2:3000`.
