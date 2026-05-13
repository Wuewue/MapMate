import { initializeApp } from "firebase/app";
import { initializeAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyDF2_hbtewhVHtiqpidGQMoweHnbjKdFDI",
  authDomain: "zenly-demo.firebaseapp.com",
  projectId: "zenly-demo",
  storageBucket: "zenly-demo.firebasestorage.app",
  messagingSenderId: "522613846463",
  appId: "1:522613846463:web:3e2398e04783812e38f10a",
  measurementId: "G-H6WMQYSSZH"
};

// 1. Initialize Firebase
const app = initializeApp(firebaseConfig);

// 2. Initialize Auth (bypassing native module errors) and Firestore
const auth = initializeAuth(app);
const db = getFirestore(app);

// 3. Export them so the rest of the app can use them!
export { auth, db };