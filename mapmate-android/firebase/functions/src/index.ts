import {setGlobalOptions} from "firebase-functions/v2";
import {CallableRequest, HttpsError, onCall} from "firebase-functions/v2/https";
import {onSchedule} from "firebase-functions/v2/scheduler";
import {initializeApp} from "firebase-admin/app";
import {getAuth} from "firebase-admin/auth";
import {getMessaging} from "firebase-admin/messaging";
import {
  DocumentData,
  FieldValue,
  Timestamp,
  getFirestore,
} from "firebase-admin/firestore";
import * as nodemailer from "nodemailer";
import * as crypto from "node:crypto";

initializeApp();

setGlobalOptions({
  region: "asia-southeast1",
  maxInstances: 20,
});

const db = getFirestore();
const auth = getAuth();
const messaging = getMessaging();

type PrivacyMode = "precise" | "blurred" | "frozen" | "hidden";
type TransportMode = "walk" | "bike" | "car" | "ship" | "still" | "unknown";
type BatteryStatus = "charging" | "low" | "full" | "normal" | "unknown";

const callableOptions = {
  enforceAppCheck: false,
};

export const requestEmailOtp = onCall(callableOptions, async (request) => {
  const email = normalizeEmail(readString(request.data.email, "email", 5, 254));
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    throw new HttpsError("invalid-argument", "Email address is invalid.");
  }

  const code = String(crypto.randomInt(0, 1_000_000)).padStart(6, "0");
  const ttlMinutes = Number(process.env.OTP_TTL_MINUTES ?? 10);

  await db.collection("emailOtps").doc(otpDocId(email)).set({
    email,
    codeHash: hashOtp(email, code),
    attempts: 0,
    expiresAt: Timestamp.fromMillis(Date.now() + ttlMinutes * 60 * 1000),
    createdAt: FieldValue.serverTimestamp(),
  });

  const transporter = getSmtpTransporter();
  if (transporter) {
    await transporter.sendMail({
      from: process.env.OTP_FROM_EMAIL ?? process.env.SMTP_USER,
      to: email,
      subject: "Your MAPMATE login code",
      text: `Your MAPMATE verification code is ${code}. It expires in ${ttlMinutes} minutes.`,
    });
    return {sent: true};
  }

  if (process.env.NODE_ENV !== "production") {
    console.log(`MAPMATE dev OTP for ${email}: ${code}`);
    return {sent: false, debugCode: code};
  }

  throw new HttpsError("failed-precondition", "Email delivery is not configured.");
});

export const verifyEmailOtp = onCall(callableOptions, async (request) => {
  const email = normalizeEmail(readString(request.data.email, "email", 5, 254));
  const code = readString(request.data.code, "code", 6, 6);
  const ref = db.collection("emailOtps").doc(otpDocId(email));
  const doc = await ref.get();

  if (!doc.exists) {
    throw new HttpsError("invalid-argument", "Verification code is invalid or expired.");
  }

  const otp = doc.data() ?? {};
  if (timestampMillis(otp.expiresAt) < Date.now()) {
    await ref.delete();
    throw new HttpsError("invalid-argument", "Verification code is invalid or expired.");
  }

  if ((otp.attempts ?? 0) >= 5) {
    await ref.delete();
    throw new HttpsError("resource-exhausted", "Too many failed attempts. Request a new code.");
  }

  if (otp.codeHash !== hashOtp(email, code)) {
    await ref.set({attempts: FieldValue.increment(1)}, {merge: true});
    throw new HttpsError("invalid-argument", "Verification code is invalid or expired.");
  }

  await ref.delete();

  let userRecord;
  try {
    userRecord = await auth.getUserByEmail(email);
  } catch {
    userRecord = await auth.createUser({email, emailVerified: true});
  }

  const customToken = await auth.createCustomToken(userRecord.uid);
  return {customToken, uid: userRecord.uid};
});

export const createProfile = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const displayName = readString(request.data.displayName, "displayName", 2, 60);
  const username = readString(request.data.username, "username", 3, 24).toLowerCase();

  if (!/^[a-z0-9_.]+$/.test(username)) {
    throw new HttpsError("invalid-argument", "Username may only contain letters, numbers, underscores, and dots.");
  }

  await db.runTransaction(async (tx) => {
    const userRef = db.collection("users").doc(uid);
    const usernameRef = db.collection("usernames").doc(username);
    const usernameDoc = await tx.get(usernameRef);

    if (usernameDoc.exists && usernameDoc.data()?.uid !== uid) {
      throw new HttpsError("already-exists", "Username is already taken.");
    }

    tx.set(userRef, {
      displayName,
      username,
      usernameLower: username,
      avatarUrl: optionalString(request.data.avatarUrl, 512),
      bio: optionalString(request.data.bio, 160),
      globalLocationSharing: true,
      batteryPercent: null,
      batteryStatus: "unknown",
      lastActiveAt: FieldValue.serverTimestamp(),
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});

    tx.set(usernameRef, {
      uid,
      createdAt: FieldValue.serverTimestamp(),
    }, {merge: true});
  });

  return {ok: true};
});

export const updateProfile = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const update: Record<string, unknown> = {
    updatedAt: FieldValue.serverTimestamp(),
  };

  const displayName = optionalString(request.data.displayName, 60);
  const avatarUrl = optionalString(request.data.avatarUrl, 512);
  const bio = optionalString(request.data.bio, 160);

  if (displayName !== null) update.displayName = displayName;
  if (avatarUrl !== null) update.avatarUrl = avatarUrl;
  if (bio !== null) update.bio = bio;

  await db.collection("users").doc(uid).set(update, {merge: true});
  return {ok: true};
});

export const searchUsers = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const query = readString(request.data.query, "query", 2, 24).toLowerCase();

  const users = await db.collection("users")
    .where("usernameLower", ">=", query)
    .where("usernameLower", "<=", `${query}\uf8ff`)
    .limit(10)
    .get();

  return {
    users: users.docs
      .filter((doc) => doc.id !== uid)
      .map((doc) => {
        const user = doc.data();
        return {
          uid: doc.id,
          displayName: user.displayName ?? "MAPMATE user",
          username: user.username ?? null,
          avatarUrl: user.avatarUrl ?? null,
          initials: initialsFromName(user.displayName ?? user.username ?? "M"),
        };
      }),
  };
});

export const sendFriendRequest = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const targetUid = readString(request.data.targetUid, "targetUid", 10, 128);

  if (uid === targetUid) {
    throw new HttpsError("invalid-argument", "You cannot add yourself.");
  }

  await db.runTransaction(async (tx) => {
    const myRef = friendRef(uid, targetUid);
    const theirRef = friendRef(targetUid, uid);
    const targetDoc = await tx.get(db.collection("users").doc(targetUid));

    if (!targetDoc.exists) {
      throw new HttpsError("not-found", "User not found.");
    }

    const [mine, theirs] = await Promise.all([tx.get(myRef), tx.get(theirRef)]);
    if (mine.data()?.status === "blocked" || theirs.data()?.status === "blocked") {
      throw new HttpsError("permission-denied", "Friend request is not allowed.");
    }

    tx.set(myRef, {
      status: "pending_sent",
      privacyMode: "precise",
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});

    tx.set(theirRef, {
      status: "pending_received",
      privacyMode: "hidden",
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});
  });

  await createNotification(targetUid, {
    type: "friend_request",
    actorUid: uid,
    title: "New friend request",
    body: "Someone wants to add you on MAPMATE.",
  });

  return {ok: true};
});

export const acceptFriendRequest = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const requesterUid = readString(request.data.requesterUid, "requesterUid", 10, 128);

  await db.runTransaction(async (tx) => {
    const myRef = friendRef(uid, requesterUid);
    const requesterRef = friendRef(requesterUid, uid);
    const mine = await tx.get(myRef);

    if (mine.data()?.status !== "pending_received") {
      throw new HttpsError("failed-precondition", "No pending request from this user.");
    }

    const acceptedFields = {
      status: "accepted",
      streakDays: 0,
      acceptedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    };

    tx.set(myRef, {
      ...acceptedFields,
      privacyMode: mine.data()?.privacyMode ?? "precise",
    }, {merge: true});
    tx.set(requesterRef, acceptedFields, {merge: true});
  });

  await Promise.all([
    createNotification(requesterUid, {
      type: "friend_request_accepted",
      actorUid: uid,
      title: "Friend request accepted",
      body: "You are now friends on MAPMATE.",
    }),
    createActivity(uid, {
      type: "friend_added",
      title: "You added a new friend",
      metadata: {friendUid: requesterUid},
    }),
    createActivity(requesterUid, {
      type: "friend_added",
      title: "You added a new friend",
      metadata: {friendUid: uid},
    }),
  ]);

  return {ok: true};
});

export const unfriendUser = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const friendUid = readString(request.data.friendUid, "friendUid", 10, 128);

  await db.runTransaction(async (tx) => {
    tx.delete(friendRef(uid, friendUid));
    tx.delete(friendRef(friendUid, uid));
  });

  return {ok: true};
});

export const blockUser = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const blockedUid = readString(request.data.blockedUid, "blockedUid", 10, 128);

  await db.runTransaction(async (tx) => {
    tx.set(friendRef(uid, blockedUid), {
      status: "blocked",
      privacyMode: "hidden",
      blockedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});

    tx.set(friendRef(blockedUid, uid), {
      status: "blocked_by_other",
      privacyMode: "hidden",
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});
  });

  return {ok: true};
});

export const updateFriendPrivacy = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const friendUid = readString(request.data.friendUid, "friendUid", 10, 128);
  const privacyMode = readPrivacyMode(request.data.privacyMode);

  await db.runTransaction(async (tx) => {
    const myRef = friendRef(uid, friendUid);
    const liveRef = db.collection("liveLocations").doc(uid);
    const friend = await tx.get(myRef);

    if (friend.data()?.status !== "accepted") {
      throw new HttpsError("failed-precondition", "Privacy can only be changed for accepted friends.");
    }

    const update: Record<string, unknown> = {
      privacyMode,
      updatedAt: FieldValue.serverTimestamp(),
    };

    if (privacyMode === "frozen") {
      const liveLocation = await tx.get(liveRef);
      if (liveLocation.exists) {
        const live = liveLocation.data() ?? {};
        update.frozenLocation = {
          lat: live.lat,
          lng: live.lng,
          accuracyMeters: live.accuracyMeters,
          frozenAt: FieldValue.serverTimestamp(),
        };
      }
    }

    tx.update(myRef, update);
  });

  return {ok: true};
});

export const updateMyLocation = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const lat = readNumber(request.data.lat, "lat", -90, 90);
  const lng = readNumber(request.data.lng, "lng", -180, 180);

  await Promise.all([
    db.collection("liveLocations").doc(uid).set({
      lat,
      lng,
      accuracyMeters: readNumber(request.data.accuracyMeters, "accuracyMeters", 0, 5000),
      speedMps: optionalNumber(request.data.speedMps, 0, 150),
      headingDegrees: optionalNumber(request.data.headingDegrees, 0, 360),
      transportMode: readTransportMode(request.data.transportMode),
      isBackground: request.data.isBackground === true,
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true}),
    db.collection("users").doc(uid).set({
      lastActiveAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true}),
  ]);

  return {ok: true};
});

export const updateBatteryStatus = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);

  await db.collection("users").doc(uid).set({
    batteryPercent: readNumber(request.data.batteryPercent, "batteryPercent", 0, 100),
    batteryStatus: readBatteryStatus(request.data.batteryStatus),
    batteryUpdatedAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  }, {merge: true});

  return {ok: true};
});

export const registerFcmToken = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const token = readString(request.data.token, "token", 20, 4096);

  await db.collection("users").doc(uid).set({
    fcmTokens: FieldValue.arrayUnion(token),
    updatedAt: FieldValue.serverTimestamp(),
  }, {merge: true});

  return {ok: true};
});

export const updateNotificationSettings = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);

  await db.collection("users").doc(uid).set({
    notificationSettings: {
      friendRequests: request.data.friendRequests !== false,
      emojis: request.data.emojis !== false,
      offGridAlerts: request.data.offGridAlerts !== false,
      locationAccuracy: request.data.locationAccuracy !== false,
    },
    updatedAt: FieldValue.serverTimestamp(),
  }, {merge: true});

  return {ok: true};
});

export const getNotifications = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const limit = clampLimit(request.data.limit);
  const notifications = await db.collection("users").doc(uid).collection("notifications")
    .orderBy("createdAt", "desc")
    .limit(limit)
    .get();

  return {
    notifications: notifications.docs.map((doc) => {
      const notification = doc.data();
      return {
        id: doc.id,
        type: notification.type ?? "notification",
        actorUid: notification.actorUid ?? null,
        title: notification.title ?? "MAPMATE",
        body: notification.body ?? "",
        read: notification.read === true,
        createdAtMillis: timestampMillis(notification.createdAt),
      };
    }),
  };
});

export const markNotificationRead = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const notificationId = readString(request.data.notificationId, "notificationId", 4, 128);

  await db.collection("users").doc(uid).collection("notifications").doc(notificationId).set({
    read: true,
    readAt: FieldValue.serverTimestamp(),
  }, {merge: true});

  return {ok: true};
});

export const getRecentActivities = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const sevenDaysAgo = Timestamp.fromMillis(Date.now() - 7 * 24 * 60 * 60 * 1000);
  const activities = await db.collection("users").doc(uid).collection("activities")
    .where("createdAt", ">=", sevenDaysAgo)
    .orderBy("createdAt", "desc")
    .limit(100)
    .get();

  return {
    activities: activities.docs.map((doc) => {
      const activity = doc.data();
      return {
        id: doc.id,
        type: activity.type ?? "activity",
        title: activity.title ?? "",
        createdAtMillis: timestampMillis(activity.createdAt),
      };
    }),
  };
});

export const getHomeMapFeed = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const currentLat = optionalNumber(request.data.currentLat, -90, 90);
  const currentLng = optionalNumber(request.data.currentLng, -180, 180);

  const friendsSnap = await db.collection("users").doc(uid).collection("friends")
    .where("status", "==", "accepted")
    .limit(100)
    .get();

  const friends = await Promise.all(friendsSnap.docs.map(async (friendDoc) => {
    const friendUid = friendDoc.id;
    const [profileDoc, privacyDoc, locationDoc] = await Promise.all([
      db.collection("users").doc(friendUid).get(),
      friendRef(friendUid, uid).get(),
      db.collection("liveLocations").doc(friendUid).get(),
    ]);

    const profile = profileDoc.data() ?? {};
    const ownerPrivacy = privacyDoc.data() ?? {};
    const visibility = ownerPrivacy.status === "accepted" ?
      normalizePrivacyMode(ownerPrivacy.privacyMode) :
      "hidden";

    return {
      uid: friendUid,
      displayName: profile.displayName ?? "Friend",
      username: profile.username ?? null,
      avatarUrl: profile.avatarUrl ?? null,
      initials: initialsFromName(profile.displayName ?? profile.username ?? "F"),
      privacyMode: visibility,
      streakDays: friendDoc.data().streakDays ?? 0,
      battery: {
        percent: profile.batteryPercent ?? null,
        status: profile.batteryStatus ?? "unknown",
        updatedAtMillis: timestampMillis(profile.batteryUpdatedAt),
      },
      lastActiveAtMillis: timestampMillis(profile.lastActiveAt),
      location: buildVisibleLocation({
        viewerUid: uid,
        ownerUid: friendUid,
        visibility,
        locationData: locationDoc.data(),
        frozenLocation: ownerPrivacy.frozenLocation,
        currentLat,
        currentLng,
      }),
    };
  }));

  friends.sort((a, b) => {
    const aDistance = a.location?.distanceMeters ?? Number.MAX_SAFE_INTEGER;
    const bDistance = b.location?.distanceMeters ?? Number.MAX_SAFE_INTEGER;
    return aDistance - bDistance;
  });

  return {
    serverTimeMillis: Date.now(),
    friends,
    nearbyFriendIds: friends
      .filter((friend) => (friend.location?.distanceMeters ?? Number.MAX_SAFE_INTEGER) <= 1000)
      .map((friend) => friend.uid),
  };
});

export const sendEmoji = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const friendUid = readString(request.data.friendUid, "friendUid", 10, 128);
  const emoji = readString(request.data.emoji, "emoji", 1, 16);

  await assertAcceptedFriends(uid, friendUid);
  await db.collection("interactions").doc(pairId(uid, friendUid)).collection("messages").add({
    type: "emoji",
    fromUid: uid,
    toUid: friendUid,
    emoji,
    createdAt: FieldValue.serverTimestamp(),
  });

  await Promise.all([
    createNotification(friendUid, {
      type: "emoji",
      actorUid: uid,
      title: "New emoji",
      body: emoji,
    }),
    createActivity(uid, {
      type: "emoji_sent",
      title: "You sent an emoji",
      metadata: {friendUid, emoji},
    }),
  ]);

  return {ok: true};
});

export const recordVisitedPlace = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const lat = readNumber(request.data.lat, "lat", -90, 90);
  const lng = readNumber(request.data.lng, "lng", -180, 180);
  const placesRef = db.collection("users").doc(uid).collection("visitedPlaces");
  const recentPlaces = await placesRef.orderBy("lastVisitedAt", "desc").limit(100).get();

  let nearest: { id: string; distanceMeters: number } | null = null;
  for (const doc of recentPlaces.docs) {
    const place = doc.data();
    const distanceMeters = haversineMeters(lat, lng, place.lat, place.lng);
    if (Number.isFinite(distanceMeters) && (!nearest || distanceMeters < nearest.distanceMeters)) {
      nearest = {id: doc.id, distanceMeters};
    }
  }

  if (nearest && nearest.distanceMeters <= 120) {
    await placesRef.doc(nearest.id).set({
      visitCount: FieldValue.increment(1),
      lastVisitedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});
    await createActivity(uid, {
      type: "place_visited",
      title: "Visited a saved place",
      metadata: {placeId: nearest.id},
    });
    return {ok: true, placeId: nearest.id, matchedExisting: true};
  }

  const newPlace = await placesRef.add({
    label: optionalString(request.data.label, 80),
    placeType: optionalString(request.data.placeType, 40) ?? "unknown",
    lat,
    lng,
    visitCount: 1,
    firstVisitedAt: FieldValue.serverTimestamp(),
    lastVisitedAt: FieldValue.serverTimestamp(),
    createdAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  });

  await createActivity(uid, {
    type: "place_visited",
    title: "Visited a new place",
    metadata: {placeId: newPlace.id},
  });

  return {ok: true, placeId: newPlace.id, matchedExisting: false};
});

export const getVisitedPlaces = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const limit = clampLimit(request.data.limit);
  const places = await db.collection("users").doc(uid).collection("visitedPlaces")
    .orderBy("lastVisitedAt", "desc")
    .limit(limit)
    .get();

  return {
    places: places.docs.map((doc) => {
      const place = doc.data();
      return {
        id: doc.id,
        label: place.label ?? null,
        placeType: place.placeType ?? "unknown",
        lat: place.lat,
        lng: place.lng,
        visitCount: place.visitCount ?? 0,
        firstVisitedAtMillis: timestampMillis(place.firstVisitedAt),
        lastVisitedAtMillis: timestampMillis(place.lastVisitedAt),
      };
    }),
  };
});

export const detectOffGridFriends = onSchedule("every 5 minutes", async () => {
  const staleBefore = Timestamp.fromMillis(Date.now() - 10 * 60 * 1000);
  const suppressBeforeMillis = Date.now() - 60 * 60 * 1000;
  const staleLocations = await db.collection("liveLocations")
    .where("updatedAt", "<", staleBefore)
    .limit(250)
    .get();

  await Promise.all(staleLocations.docs.map(async (locationDoc) => {
    const location = locationDoc.data();
    const lastAlertSentAt = timestampMillis(location.offGridAlertSentAt);
    if (lastAlertSentAt && lastAlertSentAt > suppressBeforeMillis) return;

    const ownerUid = locationDoc.id;
    const visibleFriends = await db.collection("users").doc(ownerUid).collection("friends")
      .where("status", "==", "accepted")
      .get();

    await Promise.all(visibleFriends.docs.map(async (friendDoc) => {
      if (normalizePrivacyMode(friendDoc.data().privacyMode) === "hidden") return;
      await createNotification(friendDoc.id, {
        type: "off_grid",
        actorUid: ownerUid,
        title: "Friend went off the grid",
        body: "Their phone may be offline, out of signal, or out of battery.",
      });
    }));

    await locationDoc.ref.set({
      offGridAlertSentAt: FieldValue.serverTimestamp(),
    }, {merge: true});
  }));
});

function requireUid(request: CallableRequest): string {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in is required.");
  return uid;
}

function friendRef(ownerUid: string, friendUid: string) {
  return db.collection("users").doc(ownerUid).collection("friends").doc(friendUid);
}

async function assertAcceptedFriends(uid: string, friendUid: string) {
  const [mine, theirs] = await Promise.all([
    friendRef(uid, friendUid).get(),
    friendRef(friendUid, uid).get(),
  ]);
  if (mine.data()?.status !== "accepted" || theirs.data()?.status !== "accepted") {
    throw new HttpsError("permission-denied", "Accepted friendship is required.");
  }
}

async function createNotification(targetUid: string, payload: Record<string, unknown>) {
  const target = await db.collection("users").doc(targetUid).get();
  const targetData = target.data() ?? {};
  const settings = asObject(targetData.notificationSettings);
  const type = String(payload.type ?? "notification");

  if (type === "friend_request" && settings.friendRequests === false) return;
  if (type === "emoji" && settings.emojis === false) return;
  if (type === "off_grid" && settings.offGridAlerts === false) return;

  await db.collection("users").doc(targetUid).collection("notifications").add({
    ...payload,
    read: false,
    createdAt: FieldValue.serverTimestamp(),
  });

  const tokens = (targetData.fcmTokens ?? []) as string[];
  if (tokens.length === 0) return;

  await messaging.sendEachForMulticast({
    tokens,
    notification: {
      title: String(payload.title ?? "MAPMATE"),
      body: String(payload.body ?? ""),
    },
    data: {
      type,
      actorUid: String(payload.actorUid ?? ""),
    },
  });
}

async function createActivity(targetUid: string, payload: Record<string, unknown>) {
  await db.collection("users").doc(targetUid).collection("activities").add({
    ...payload,
    createdAt: FieldValue.serverTimestamp(),
  });
}

function buildVisibleLocation(input: {
  viewerUid: string;
  ownerUid: string;
  visibility: PrivacyMode;
  locationData: DocumentData | undefined;
  frozenLocation: unknown;
  currentLat: number | null;
  currentLng: number | null;
}) {
  const {viewerUid, ownerUid, visibility, locationData, frozenLocation, currentLat, currentLng} = input;
  if (visibility === "hidden" || !locationData) return null;
  if (typeof locationData.lat !== "number" || typeof locationData.lng !== "number") return null;

  const updatedAtMillis = timestampMillis(locationData.updatedAt);
  let lat = locationData.lat;
  let lng = locationData.lng;
  let accuracyMeters = locationData.accuracyMeters ?? null;

  if (visibility === "frozen") {
    const frozen = asObject(frozenLocation);
    lat = frozen.lat ?? lat;
    lng = frozen.lng ?? lng;
    accuracyMeters = frozen.accuracyMeters ?? accuracyMeters;
  }

  if (visibility === "blurred") {
    const blurred = blurCoordinates(lat, lng, viewerUid, ownerUid);
    lat = blurred.lat;
    lng = blurred.lng;
    accuracyMeters = Math.max(Number(accuracyMeters ?? 0), blurred.accuracyMeters);
  }

  return {
    lat,
    lng,
    accuracyMeters,
    visibility,
    isStale: updatedAtMillis ? Date.now() - updatedAtMillis > 10 * 60 * 1000 : true,
    updatedAtMillis,
    distanceMeters: currentLat !== null && currentLng !== null ?
      haversineMeters(currentLat, currentLng, lat, lng) :
      null,
    speedMps: locationData.speedMps ?? null,
    headingDegrees: locationData.headingDegrees ?? null,
    transportMode: locationData.transportMode ?? "unknown",
  };
}

function blurCoordinates(lat: number, lng: number, viewerUid: string, ownerUid: string) {
  const seed = hash(`${viewerUid}:${ownerUid}:${new Date().toISOString().slice(0, 10)}`);
  const angle = ((seed % 360) * Math.PI) / 180;
  const radiusMeters = 350 + (seed % 450);
  return {
    lat: lat + (Math.cos(angle) * radiusMeters) / 111320,
    lng: lng + (Math.sin(angle) * radiusMeters) / (111320 * Math.cos((lat * Math.PI) / 180)),
    accuracyMeters: radiusMeters,
  };
}

function haversineMeters(aLat: number, aLng: number, bLat: number, bLng: number): number {
  if (![aLat, aLng, bLat, bLng].every(Number.isFinite)) return Number.NaN;
  const earthRadiusMeters = 6371000;
  const dLat = toRadians(bLat - aLat);
  const dLng = toRadians(bLng - aLng);
  const lat1 = toRadians(aLat);
  const lat2 = toRadians(bLat);
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1) * Math.cos(lat2) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2);
  return earthRadiusMeters * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function toRadians(value: number) {
  return value * Math.PI / 180;
}

function hash(value: string): number {
  return Math.abs(crypto.createHash("sha256").update(value).digest().readInt32BE(0));
}

function pairId(a: string, b: string): string {
  return [a, b].sort().join("_");
}

function timestampMillis(value: unknown): number {
  return value instanceof Timestamp ? value.toMillis() : 0;
}

function initialsFromName(name: string): string {
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase() ?? "").join("") || "F";
}

function readString(value: unknown, field: string, min: number, max: number): string {
  if (typeof value !== "string") throw new HttpsError("invalid-argument", `${field} must be a string.`);
  const trimmed = value.trim();
  if (trimmed.length < min || trimmed.length > max) {
    throw new HttpsError("invalid-argument", `${field} length is invalid.`);
  }
  return trimmed;
}

function optionalString(value: unknown, max: number): string | null {
  if (value === null || value === undefined || value === "") return null;
  if (typeof value !== "string" || value.length > max) {
    throw new HttpsError("invalid-argument", "String value is invalid.");
  }
  return value.trim();
}

function readNumber(value: unknown, field: string, min: number, max: number): number {
  if (typeof value !== "number" || !Number.isFinite(value) || value < min || value > max) {
    throw new HttpsError("invalid-argument", `${field} is invalid.`);
  }
  return value;
}

function optionalNumber(value: unknown, min: number, max: number): number | null {
  if (value === null || value === undefined) return null;
  if (typeof value !== "number" || !Number.isFinite(value) || value < min || value > max) {
    throw new HttpsError("invalid-argument", "Number value is invalid.");
  }
  return value;
}

function readPrivacyMode(value: unknown): PrivacyMode {
  if (value === "precise" || value === "blurred" || value === "frozen" || value === "hidden") return value;
  throw new HttpsError("invalid-argument", "Privacy mode is invalid.");
}

function normalizePrivacyMode(value: unknown): PrivacyMode {
  if (value === "precise" || value === "blurred" || value === "frozen" || value === "hidden") return value;
  return "hidden";
}

function readTransportMode(value: unknown): TransportMode {
  if (value === "walk" || value === "bike" || value === "car" || value === "ship" || value === "still") return value;
  return "unknown";
}

function readBatteryStatus(value: unknown): BatteryStatus {
  if (value === "charging" || value === "low" || value === "full" || value === "normal") return value;
  return "unknown";
}

function asObject(value: unknown): Record<string, any> {
  if (value && typeof value === "object" && !Array.isArray(value)) return value as Record<string, any>;
  return {};
}

function clampLimit(value: unknown): number {
  const parsed = Math.floor(Number(value ?? 30));
  if (!Number.isFinite(parsed)) return 30;
  return Math.min(Math.max(parsed, 1), 100);
}

function normalizeEmail(email: string): string {
  return email.trim().toLowerCase();
}

function otpDocId(email: string): string {
  return crypto.createHash("sha256").update(email).digest("hex");
}

function hashOtp(email: string, code: string): string {
  return crypto.createHash("sha256")
    .update(`${email}:${code}:${process.env.OTP_HASH_SECRET ?? "mapmate-dev-secret"}`)
    .digest("hex");
}

function getSmtpTransporter() {
  if (!process.env.SMTP_HOST || !process.env.SMTP_USER || !process.env.SMTP_PASS) return null;
  return nodemailer.createTransport({
    host: process.env.SMTP_HOST,
    port: Number(process.env.SMTP_PORT ?? 587),
    secure: process.env.SMTP_SECURE === "true",
    auth: {
      user: process.env.SMTP_USER,
      pass: process.env.SMTP_PASS,
    },
  });
}
