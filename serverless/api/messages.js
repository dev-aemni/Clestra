/**
 * Clestra Serverless Offline Relay Backend
 * 
 * Suitable for Vercel, Netlify, or Cloudflare Node.js runtimes.
 * Connects to MongoDB Atlas Free Tier (M0 - 512MB limit).
 * 
 * HOW TO SET UP MONGODB ATLAS:
 * 1. Create a free cluster at https://www.mongodb.com/cloud/atlas (Shared M0 - Free forever)
 * 2. Create a Database User with read/write access.
 * 3. Network Access -> Add IP Address -> Allow Access from Anywhere (0.0.0.0/0) for serverless environments.
 * 4. Get Connection String: "mongodb+srv://<username>:<password>@cluster0.abcde.mongodb.net/clestra_relay?retryWrites=true&w=majority"
 * 5. Set MONGODB_URI in your Vercel/Netlify Environment Variables or .env file.
 */

const { MongoClient } = require('mongodb');

// Connection pooling across serverless warm containers
let cachedClient = null;
let cachedDb = null;

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/clestra_relay';
const DB_NAME = process.env.DB_NAME || 'clestra_relay';
const COLLECTION_NAME = 'pending_messages';

/**
 * Connect to MongoDB with cached client to minimize memory overhead
 * and connection handshake latency in serverless environments.
 */
async function connectToDatabase() {
  if (cachedClient && cachedDb) {
    return { client: cachedClient, db: cachedDb };
  }

  if (!process.env.MONGODB_URI) {
    console.warn('⚠️ MONGODB_URI environment variable is not set. Using fallback:', MONGODB_URI);
  }

  const client = new MongoClient(MONGODB_URI, {
    maxPoolSize: 10,
    minPoolSize: 0,
    serverSelectionTimeoutMS: 5000,
    connectTimeoutMS: 10000,
  });

  await client.connect();
  const db = client.db(DB_NAME);

  // Ensure index on recipientId for O(1) lightning lookup & auto-cleanup
  await db.collection(COLLECTION_NAME).createIndex({ recipientId: 1, timestamp: 1 });

  cachedClient = client;
  cachedDb = db;
  return { client, db };
}

/**
 * Main Serverless API Handler
 * Handles:
 * - POST /api/messages/send (Offline Store)
 * - GET  /api/messages/sync (Sync & Auto-Cleanup)
 * - GET  /api/health
 */
module.exports = async (req, res) => {
  // CORS Headers for cross-origin access from mobile / web apps
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  // Parse path for Vercel/Netlify
  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = url.pathname;

  try {
    const { db } = await connectToDatabase();
    const collection = db.collection(COLLECTION_NAME);

    // 1. POST /api/messages/send - Offline Message Store
    if (req.method === 'POST' && (pathname.endsWith('/send') || pathname === '/api/messages')) {
      const { senderId, recipientId, compressedContent, timestamp } = req.body || {};

      if (!senderId || !recipientId || !compressedContent) {
        return res.status(400).json({
          success: false,
          error: 'Missing required fields: senderId, recipientId, compressedContent',
        });
      }

      const pendingRecord = {
        senderId: String(senderId),
        recipientId: String(recipientId),
        compressedContent: String(compressedContent),
        timestamp: Number(timestamp) || Date.now(),
        delivered: false,
        createdAt: new Date(),
      };

      const result = await collection.insertOne(pendingRecord);

      return res.status(201).json({
        success: true,
        messageId: result.insertedId,
        status: 'queued_offline_serverless',
      });
    }

    // 2. GET /api/messages/sync - Sync & Auto-Cleanup
    if (req.method === 'GET' && (pathname.endsWith('/sync') || pathname === '/api/messages')) {
      const recipientId = url.searchParams.get('recipientId') || req.query?.recipientId;

      if (!recipientId) {
        return res.status(400).json({
          success: false,
          error: 'Missing required query parameter: recipientId',
        });
      }

      // Query all pending messages for this recipient
      const pendingMessages = await collection
        .find({ recipientId: String(recipientId) })
        .sort({ timestamp: 1 })
        .toArray();

      // Format payload to return
      const messagesToReturn = pendingMessages.map((msg) => ({
        id: msg._id.toString(),
        senderId: msg.senderId,
        recipientId: msg.recipientId,
        compressedContent: msg.compressedContent,
        timestamp: msg.timestamp,
        delivered: true, // will be marked delivered now
      }));

      // AUTO-CLEANUP: Immediately delete fetched messages from MongoDB
      // Keeps active database usage near 0 MB to stay well within MongoDB Atlas 512MB free tier
      if (pendingMessages.length > 0) {
        const idsToDelete = pendingMessages.map((msg) => msg._id);
        await collection.deleteMany({ _id: { $in: idsToDelete } });
        console.log(`Cleaned up ${idsToDelete.length} synced messages for recipient: ${recipientId}`);
      }

      return res.status(200).json({
        success: true,
        recipientId,
        count: messagesToReturn.length,
        messages: messagesToReturn,
      });
    }

    // 3. Health check
    if (req.method === 'GET') {
      return res.status(200).json({
        service: 'Clestra Serverless Offline Relay',
        status: 'online',
        mongodb: 'connected',
        timestamp: Date.now(),
      });
    }

    return res.status(405).json({ error: 'Method not allowed' });
  } catch (error) {
    console.error('Clestra Relay Database Error:', error);
    return res.status(500).json({
      success: false,
      error: 'Database operation failed',
      details: error.message,
    });
  }
};
