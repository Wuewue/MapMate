import React, { useState, useEffect } from 'react';
import { StyleSheet, View, Text, TouchableOpacity, ScrollView, Modal, TextInput, Alert } from 'react-native';
import { auth, db } from '../../utils/firebase';
import { doc, onSnapshot, collection, updateDoc, arrayUnion, arrayRemove } from 'firebase/firestore';
import { Ionicons, MaterialCommunityIcons, FontAwesome5 } from '@expo/vector-icons';
import { router } from 'expo-router';

export default function ProfileScreen() {
  const [userData, setUserData] = useState<any>(null);
  const [usersList, setUsersList] = useState<any[]>([]);
  
  // Friend Request States
  const [showAddModal, setShowAddModal] = useState(false);
  const [addEmail, setAddEmail] = useState('');

  useEffect(() => {
    if (!auth.currentUser) return;
    
    // Listen to our own profile
    const unsubUser = onSnapshot(doc(db, 'users', auth.currentUser.uid), (docSnap) => {
      if (docSnap.exists()) setUserData(docSnap.data());
    });

    // Listen to all users so we can search by email & map UIDs to emails for requests
    const unsubUsers = onSnapshot(collection(db, 'users'), (snapshot) => {
      const users: any[] = [];
      snapshot.forEach(d => users.push({ id: d.id, ...d.data() }));
      setUsersList(users);
    });

    return () => {
      unsubUser();
      unsubUsers();
    };
  }, []);

  // --- FRIEND REQUEST LOGIC ---
  const handleSendRequest = async () => {
    if (!addEmail.trim()) return;
    
    const targetUser = usersList.find(u => u.email.toLowerCase() === addEmail.toLowerCase().trim());
    const myUid = auth.currentUser?.uid;
    
    if (!targetUser) {
      Alert.alert('Not Found', 'Could not find a user with this email.');
      return;
    }
    if (targetUser.id === myUid) {
      Alert.alert('Error', 'You cannot add yourself.');
      return;
    }
    if (userData?.friends?.includes(targetUser.id)) {
      Alert.alert('Error', 'You are already friends!');
      return;
    }
    if (targetUser.friendRequests?.includes(myUid)) {
      Alert.alert('Pending', 'You already sent a request to this user.');
      return;
    }

    // Add our UID to the target user's friendRequests array
    try {
      await updateDoc(doc(db, 'users', targetUser.id), {
        friendRequests: arrayUnion(myUid)
      });
      Alert.alert('Success', `Friend request sent to ${targetUser.email}!`);
      setShowAddModal(false);
      setAddEmail('');
    } catch (e) {
      Alert.alert('Error', 'Failed to send request.');
    }
  };

  const handleAcceptRequest = async (requesterUid: string) => {
    const myUid = auth.currentUser?.uid;
    if (!myUid) return;
    
    try {
      // Add each other to 'friends' arrays and remove from 'friendRequests'
      await updateDoc(doc(db, 'users', myUid), {
        friends: arrayUnion(requesterUid),
        friendRequests: arrayRemove(requesterUid)
      });
      await updateDoc(doc(db, 'users', requesterUid), {
        friends: arrayUnion(myUid)
      });
      Alert.alert('Accepted', 'You are now friends!');
    } catch (e) {
      console.log(e);
    }
  };

  const handleRejectRequest = async (requesterUid: string) => {
    const myUid = auth.currentUser?.uid;
    if (!myUid) return;
    
    try {
      await updateDoc(doc(db, 'users', myUid), {
        friendRequests: arrayRemove(requesterUid)
      });
    } catch (e) {
      console.log(e);
    }
  };

  const displayInitials = userData?.email ? userData.email.substring(0, 2).toLowerCase() : 'tt';
  const displayName = userData?.email ? userData.email.split('@')[0] : 'tt';
  const pendingRequests = userData?.friendRequests || [];

  return (
    <View style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        
        {/* Top Header Icons */}
        <View style={styles.headerIcons}>
          <TouchableOpacity style={{marginRight: 'auto'}}>
             {/* Match stat display if needed, leaving blank for layout match */}
          </TouchableOpacity>
          <TouchableOpacity style={styles.iconBtn}><Ionicons name="shield-checkmark" size={24} color="#4de8b4" /></TouchableOpacity>
          
          {/* OPEN ADD FRIEND MODAL BUTTON */}
          <TouchableOpacity style={styles.iconBtn} onPress={() => setShowAddModal(true)}>
            <Ionicons name="person-add" size={24} color="#fff" />
            {pendingRequests.length > 0 && <View style={styles.notifDot} />}
          </TouchableOpacity>
          
          <TouchableOpacity style={styles.iconBtn}><Ionicons name="settings" size={24} color="#fff" /></TouchableOpacity>
        </View>

        {/* Profile Info */}
        <View style={styles.profileHeader}>
          <View style={styles.avatarBox}>
            <View style={styles.addAvatarBtn}><Ionicons name="add" size={16} color="#000" /></View>
            <Text style={styles.avatarText}>{displayInitials}</Text>
          </View>
          <View style={styles.profileInfo}>
            <Text style={styles.profileName}>{displayName} 👑</Text>
            <Text style={styles.profileId}>ID: 35996758 🔲</Text>
            <View style={styles.locationBadge}>
              <Text style={styles.locationText}>🏠 Chỗ ở</Text>
            </View>
          </View>
        </View>

        {/* Upgrade Banner */}
        <View style={styles.upgradeBanner}>
          <View style={{flex: 1, paddingRight: 10}}>
            <Text style={styles.upgradeTitle}>Solo Plan 👑</Text>
            <Text style={styles.upgradeDesc}>Check real-time locations, share your routes, and 10+ more perks!</Text>
          </View>
          <TouchableOpacity style={styles.upgradeBtn}>
            <Text style={styles.upgradeBtnText}>Upgrade</Text>
          </TouchableOpacity>
        </View>

        {/* PENDING FRIEND REQUESTS SECTION */}
        {pendingRequests.length > 0 && (
          <View style={styles.requestsContainer}>
            <Text style={styles.requestsTitle}>Friend Requests ({pendingRequests.length})</Text>
            {pendingRequests.map((uid: string) => {
              const reqUser = usersList.find(u => u.id === uid);
              if (!reqUser) return null;
              
              return (
                <View key={uid} style={styles.requestRow}>
                  <View style={styles.reqAvatar}><Text style={{color:'#fff'}}>{reqUser.email.substring(0,2).toUpperCase()}</Text></View>
                  <Text style={styles.reqEmail}>{reqUser.email}</Text>
                  <View style={{flexDirection: 'row', gap: 10}}>
                    <TouchableOpacity onPress={() => handleAcceptRequest(uid)} style={styles.btnAccept}>
                      <Ionicons name="checkmark" size={18} color="#000" />
                    </TouchableOpacity>
                    <TouchableOpacity onPress={() => handleRejectRequest(uid)} style={styles.btnReject}>
                      <Ionicons name="close" size={18} color="#fff" />
                    </TouchableOpacity>
                  </View>
                </View>
              )
            })}
          </View>
        )}

        {/* Stats Row */}
        <View style={styles.statsRow}>
          <View style={[styles.statCard, styles.visitorCard]}>
            <Text style={styles.statTitle}>Visitor activity</Text>
            <Text style={styles.statSub}>Visits: 0</Text>
            <View style={{flex: 1, justifyContent: 'center', alignItems: 'center', marginTop: 10}}>
               <View style={styles.mysteryBox}><Text style={{fontSize: 24, fontWeight: 'bold'}}>?</Text></View>
               <Text style={styles.visitorText}>Mysterious{'\n'}visitor</Text>
            </View>
          </View>

          <View style={[styles.statCard, styles.worldCard]}>
            <Text style={styles.statTitle}>Your world</Text>
            <View style={styles.worldStats}>
              <View style={{alignItems: 'center'}}>
                <Text style={styles.worldNum}>1</Text>
                <Text style={styles.worldLabel}>Cities</Text>
              </View>
              <View style={{alignItems: 'center'}}>
                <Text style={styles.worldNum}>1</Text>
                <Text style={styles.worldLabel}>Countries/Regions</Text>
              </View>
            </View>
            <View style={styles.globeGraphic}>
               <Ionicons name="earth" size={80} color="#34C759" opacity={0.8} />
            </View>
          </View>
        </View>

        {/* Grid Menu */}
        <View style={styles.gridMenu}>
          <MenuItem icon="map" label="Route" color="#4de8b4" />
          <MenuItem icon="diamond" label="Ring Shop" color="#ffb547" />
          <MenuItem icon="happy" label="My Stickers" color="#7c6fff" badge="NEW" />
          <MenuItem icon="phone-portrait" label="My Launch Screen" color="#ff6b9d" />
          <MenuItem icon="location" label="Poop Map" color="#ff6b9d" />
          <MenuItem icon="heart" label="Close Relationships" color="#ff6b9d" />
          <MenuItem icon="snow" label="Footprint Fridge" color="#eee" badge="NEW" />
          <MenuItem icon="calendar" label="Anniversaries" color="#ffb547" />
        </View>

      </ScrollView>

      {/* Bottom Nav Bar */}
      <View style={styles.bottomNav}>
        <TouchableOpacity style={styles.bottomNavItem}>
          <Ionicons name="ghost" size={24} color="#666" />
          <Text style={styles.bottomNavText}>Ghost mode</Text>
        </TouchableOpacity>
        
        <TouchableOpacity style={styles.closeBtn} onPress={() => router.push('/')}>
          <Ionicons name="close" size={32} color="#fff" />
        </TouchableOpacity>

        <TouchableOpacity style={styles.bottomNavItem}>
          <Ionicons name="chatbubble-ellipses" size={24} color="#666" />
          <Text style={styles.bottomNavText}>Contact Us</Text>
        </TouchableOpacity>
      </View>

      {/* ADD FRIEND MODAL */}
      <Modal visible={showAddModal} transparent animationType="slide">
        <View style={styles.modalBg}>
          <View style={styles.modalBox}>
            <Text style={styles.modalTitle}>Add a Friend</Text>
            <Text style={styles.modalSub}>Enter their Zenly email below to send a friend request.</Text>
            
            <TextInput 
              style={styles.modalInput}
              placeholder="friend@email.com"
              placeholderTextColor="#666"
              autoCapitalize="none"
              keyboardType="email-address"
              value={addEmail}
              onChangeText={setAddEmail}
            />

            <View style={styles.modalBtnRow}>
              <TouchableOpacity style={styles.modalBtnCancel} onPress={() => {setShowAddModal(false); setAddEmail('');}}>
                <Text style={{color: '#fff', fontWeight: 'bold'}}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.modalBtnSend} onPress={handleSendRequest}>
                <Text style={{color: '#000', fontWeight: 'bold'}}>Send Request</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

    </View>
  );
}

function MenuItem({icon, label, color, badge}: {icon: any, label: string, color: string, badge?: string}) {
  return (
    <View style={styles.menuItem}>
      <View style={styles.menuIconBox}>
        <Ionicons name={icon} size={32} color={color} />
        {badge && <View style={styles.badge}><Text style={styles.badgeText}>{badge}</Text></View>}
      </View>
      <Text style={styles.menuLabel}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#000' },
  scrollContent: { padding: 20, paddingBottom: 100, paddingTop: 50 },
  
  /* Header */
  headerIcons: { flexDirection: 'row', justifyContent: 'flex-end', alignItems: 'center', marginBottom: 20 },
  iconBtn: { marginLeft: 20, position: 'relative' },
  notifDot: { position: 'absolute', top: -2, right: -2, width: 10, height: 10, borderRadius: 5, backgroundColor: '#FF3B30', borderWidth: 2, borderColor: '#000' },

  /* Profile Header */
  profileHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: 24 },
  avatarBox: { width: 80, height: 80, borderRadius: 20, backgroundColor: '#333', justifyContent: 'center', alignItems: 'center' },
  addAvatarBtn: { position: 'absolute', top: -8, left: -8, backgroundColor: '#fff', borderRadius: 12, padding: 2 },
  avatarText: { color: '#aaa', fontSize: 24, fontWeight: 'bold' },
  profileInfo: { marginLeft: 20 },
  profileName: { color: '#fff', fontSize: 24, fontWeight: 'bold', marginBottom: 4 },
  profileId: { color: '#888', fontSize: 13, marginBottom: 8 },
  locationBadge: { backgroundColor: '#222', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 20, alignSelf: 'flex-start' },
  locationText: { color: '#ddd', fontSize: 13, fontWeight: 'bold' },

  /* Upgrade Banner */
  upgradeBanner: { 
    flexDirection: 'row', alignItems: 'center', backgroundColor: '#0A2540', 
    borderRadius: 16, padding: 20, marginBottom: 24,
    borderWidth: 1, borderColor: '#1F4068'
  },
  upgradeTitle: { color: '#fff', fontSize: 18, fontWeight: 'bold', marginBottom: 6 },
  upgradeDesc: { color: '#8BA1B7', fontSize: 13, lineHeight: 18 },
  upgradeBtn: { backgroundColor: '#fff', paddingHorizontal: 16, paddingVertical: 10, borderRadius: 20 },
  upgradeBtnText: { color: '#000', fontWeight: 'bold', fontSize: 14 },

  /* Friend Requests */
  requestsContainer: { backgroundColor: '#1A1A1A', borderRadius: 16, padding: 16, marginBottom: 24 },
  requestsTitle: { color: '#fff', fontSize: 16, fontWeight: 'bold', marginBottom: 12 },
  requestRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#222', padding: 12, borderRadius: 12, marginBottom: 8 },
  reqAvatar: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#555', alignItems: 'center', justifyContent: 'center' },
  reqEmail: { color: '#fff', flex: 1, marginLeft: 10, fontSize: 14, fontWeight: '500' },
  btnAccept: { backgroundColor: '#4de8b4', width: 32, height: 32, borderRadius: 16, alignItems: 'center', justifyContent: 'center' },
  btnReject: { backgroundColor: '#FF3B30', width: 32, height: 32, borderRadius: 16, alignItems: 'center', justifyContent: 'center' },

  /* Stats Row */
  statsRow: { flexDirection: 'row', gap: 16, marginBottom: 24 },
  statCard: { flex: 1, backgroundColor: '#111', borderRadius: 20, padding: 16, height: 180, overflow: 'hidden' },
  visitorCard: { borderWidth: 2, borderColor: '#332', shadowColor: '#FF6B9D', shadowOpacity: 0.2, shadowRadius: 10, elevation: 5 },
  worldCard: { backgroundColor: '#111' },
  statTitle: { color: '#fff', fontSize: 16, fontWeight: 'bold', marginBottom: 4 },
  statSub: { color: '#666', fontSize: 12 },
  mysteryBox: { backgroundColor: '#f0f0f5', width: 40, height: 40, borderRadius: 10, justifyContent: 'center', alignItems: 'center', marginBottom: 10, transform: [{rotate: '-10deg'}] },
  visitorText: { color: '#fff', fontSize: 14, fontWeight: 'bold', textAlign: 'center' },
  worldStats: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 20, paddingHorizontal: 10, zIndex: 10 },
  worldNum: { color: '#fff', fontSize: 24, fontWeight: 'bold' },
  worldLabel: { color: '#888', fontSize: 10, marginTop: 4 },
  globeGraphic: { position: 'absolute', bottom: -20, right: -10, opacity: 0.5 },

  /* Grid Menu */
  gridMenu: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between' },
  menuItem: { width: '23%', alignItems: 'center', marginBottom: 24 },
  menuIconBox: { width: 60, height: 60, borderRadius: 20, backgroundColor: '#1A1A1A', justifyContent: 'center', alignItems: 'center', marginBottom: 8 },
  menuLabel: { color: '#ccc', fontSize: 11, textAlign: 'center', fontWeight: '500' },
  badge: { position: 'absolute', top: -5, right: -5, backgroundColor: '#FF3B30', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 10 },
  badgeText: { color: '#fff', fontSize: 8, fontWeight: 'bold' },

  /* Bottom Nav */
  bottomNav: {
    position: 'absolute', bottom: 0, left: 0, right: 0, 
    flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center',
    backgroundColor: '#050505', paddingBottom: 30, paddingTop: 15,
    borderTopWidth: 1, borderTopColor: '#1A1A1A'
  },
  bottomNavItem: { alignItems: 'center', flex: 1 },
  bottomNavText: { color: '#666', fontSize: 11, marginTop: 4, fontWeight: 'bold' },
  closeBtn: {
    width: 60, height: 60, borderRadius: 30, backgroundColor: '#333', 
    justifyContent: 'center', alignItems: 'center', marginTop: -30,
    borderWidth: 4, borderColor: '#000'
  },

  /* Modal */
  modalBg: { flex: 1, backgroundColor: 'rgba(0,0,0,0.8)', justifyContent: 'center', alignItems: 'center', padding: 20 },
  modalBox: { backgroundColor: '#1A1A1A', width: '100%', borderRadius: 24, padding: 24, borderWidth: 1, borderColor: '#333' },
  modalTitle: { color: '#fff', fontSize: 20, fontWeight: 'bold', marginBottom: 8 },
  modalSub: { color: '#888', fontSize: 14, marginBottom: 20 },
  modalInput: { backgroundColor: '#000', color: '#fff', padding: 16, borderRadius: 12, fontSize: 16, marginBottom: 20, borderWidth: 1, borderColor: '#333' },
  modalBtnRow: { flexDirection: 'row', gap: 12 },
  modalBtnCancel: { flex: 1, backgroundColor: '#333', padding: 16, borderRadius: 12, alignItems: 'center' },
  modalBtnSend: { flex: 1, backgroundColor: '#4de8b4', padding: 16, borderRadius: 12, alignItems: 'center' }
});
