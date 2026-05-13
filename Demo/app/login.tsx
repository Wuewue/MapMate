import React, { useState } from 'react';
import { StyleSheet, View, Text, TextInput, TouchableOpacity, Alert } from 'react-native';
import { auth, db } from '../utils/firebase';
import { signInWithEmailAndPassword, createUserWithEmailAndPassword } from 'firebase/auth';
import { doc, setDoc } from 'firebase/firestore';
import { router } from 'expo-router';

export default function LoginScreen() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('password123'); // Default password for testing
  const [loading, setLoading] = useState(false);

  async function handleLogin() {
    setLoading(true);
    
    try {
      // 1. Try to sign in first
      await signInWithEmailAndPassword(auth, email.toLowerCase(), password);
      // _layout.tsx will handle the redirect
    } catch (error: any) {
      // 2. If user doesn't exist, sign them up!
      if (error.code === 'auth/user-not-found' || error.code === 'auth/invalid-credential') {
        try {
          const userCredential = await createUserWithEmailAndPassword(auth, email.toLowerCase(), password);
          
          // IMPORTANT: Create their user profile in the database!
          await setDoc(doc(db, 'users', userCredential.user.uid), {
            uid: userCredential.user.uid,
            email: email.toLowerCase(),
            ghost_mode: 'precise',
            friends: [] // Start with no friends
          });

          // _layout.tsx will handle the redirect
        } catch (signUpError: any) {
          Alert.alert('Sign Up Error', signUpError.message);
        }
      } else {
        Alert.alert('Error', error.message);
      }
    }
    
    setLoading(false);
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Zenly Clone (Firebase)</Text>
      <Text style={styles.subtitle}>Direct Sign In (Auto Account Creation)</Text>
      
      <TextInput
        style={styles.input}
        placeholder="Email address"
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
      />

      <TextInput
        style={styles.input}
        placeholder="Password"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
      />

      <TouchableOpacity style={styles.button} onPress={handleLogin} disabled={loading || !email}>
        <Text style={styles.buttonText}>{loading ? 'Signing In...' : 'Instant Sign In'}</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, justifyContent: 'center', backgroundColor: '#fff' },
  title: { fontSize: 32, fontWeight: 'bold', textAlign: 'center', marginBottom: 10, color: '#FFCA28' }, // Firebase Yellow
  subtitle: { fontSize: 16, textAlign: 'center', marginBottom: 30, color: '#666' },
  input: { borderWidth: 1, borderColor: '#E5E5EA', padding: 15, borderRadius: 10, marginBottom: 15, fontSize: 16, backgroundColor: '#F2F2F7' },
  button: { backgroundColor: '#FFCA28', padding: 15, borderRadius: 10, alignItems: 'center' },
  buttonText: { color: '#333', fontSize: 18, fontWeight: 'bold' },
});
