/**
 * science.est.center - One-Time Admin Account Setup Script
 *
 * This script initializes the primary administrator account (admin / admin)
 * directly in Firebase Authentication and Cloud Firestore.
 *
 * Prerequisites:
 * 1. Node.js installed (v18+)
 * 2. Download your Firebase Admin Service Account key from:
 *    Firebase Console -> Project Settings -> Service accounts -> Generate new private key
 *    Save it as "serviceAccountKey.json" in this directory.
 *
 * Execution:
 *   npm install firebase-admin
 *   node setup_admin.js
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const serviceAccountPath = path.join(__dirname, 'serviceAccountKey.json');

if (!fs.existsSync(serviceAccountPath)) {
  console.error('\n❌ ERROR: serviceAccountKey.json not found!');
  console.log('Please download your Service Account JSON from Firebase Console:');
  console.log('Project Settings -> Service accounts -> Generate new private key');
  console.log('Save it as "serviceAccountKey.json" next to setup_admin.js and run again.\n');
  process.exit(1);
}

const serviceAccount = require(serviceAccountPath);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const auth = admin.auth();
const db = admin.firestore();

// Fixed admin credentials
const ADMIN_USERNAME = 'admin';
const ADMIN_EMAIL = 'admin@school.app';
// The internal secret suffix ensures >= 6 characters for Firebase Auth requirements
const ADMIN_PASSWORD = 'admin__school_est';

async function setupFirstAdmin() {
  console.log('🚀 Initializing science.est.center primary admin account...');

  let uid;
  try {
    // Check if Auth user already exists
    const existingUser = await auth.getUserByEmail(ADMIN_EMAIL);
    uid = existingUser.uid;
    console.log(`ℹ️ Auth user already exists with UID: ${uid}`);

    // Update password to match default
    await auth.updateUser(uid, {
      password: ADMIN_PASSWORD,
      displayName: 'مدير النظام'
    });
  } catch (error) {
    if (error.code === 'auth/user-not-found') {
      const newUser = await auth.createUser({
        email: ADMIN_EMAIL,
        password: ADMIN_PASSWORD,
        displayName: 'مدير النظام'
      });
      uid = newUser.uid;
      console.log(`✅ Auth user created successfully with UID: ${uid}`);
    } else {
      throw error;
    }
  }

  // Create or update Firestore user document
  const adminUserData = {
    id: uid,
    username: ADMIN_USERNAME,
    fullName: 'مدير النظام',
    role: 'ADMIN',
    phone: '',
    isActive: true,
    isPrimaryAdmin: true,
    needsPasswordChange: true, // Forces "Secure your account" screen on first login
    recoveryEmail: '',
    groupIds: [],
    subjectIds: [],
    teacherPermissions: {
      canPublishResources: true,
      canEditGrades: true,
      canSendAnnouncements: true
    },
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await db.collection('users').document(uid).set(adminUserData, { merge: true });
  console.log(`✅ Firestore user profile created at users/${uid}`);

  console.log('\n======================================================');
  console.log('🎉 SETUP COMPLETE!');
  console.log('------------------------------------------------------');
  console.log(`Username : ${ADMIN_USERNAME}`);
  console.log(`Password : admin`);
  console.log('On first login, you will be prompted to set a permanent');
  console.log('secure password and recovery email.');
  console.log('======================================================\n');
  process.exit(0);
}

setupFirstAdmin().catch((err) => {
  console.error('❌ Failed to setup admin account:', err);
  process.exit(1);
});
