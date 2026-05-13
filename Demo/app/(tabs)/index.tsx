import React, { useState, useEffect, useRef } from 'react';
import { StyleSheet, View, Text, ActivityIndicator, TouchableOpacity, Dimensions } from 'react-native';
import MapView, { Marker, PROVIDER_DEFAULT } from 'react-native-maps';
import * as Location from 'expo-location';
import * as Battery from 'expo-battery';
import { auth, db } from '../../utils/firebase';
import { collection, doc, setDoc, onSnapshot } from 'firebase/firestore';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { LinearGradient } from 'expo-linear-gradient';

// Dark map style JSON
const darkMapStyle = [
  { "elementType": "geometry", "stylers": [{"color": "#1a1d28"}] },
  { "elementType": "labels.icon", "stylers": [{"visibility": "off"}] },
  { "elementType": "labels.text.fill", "stylers": [{"color": "#6b6f82"}] },
  { "elementType": "labels.text.stroke", "stylers": [{"color": "#1a1d28"}] },
  { "featureType": "administrative.land_parcel", "elementType": "labels.text.fill", "stylers": [{"color": "#bdbdbd"}] },
  { "featureType": "poi", "elementType": "geometry", "stylers": [{"color": "#1c1f2a"}] },
  { "featureType": "road", "elementType": "geometry", "stylers": [{"color": "#2a2d3d"}] },
  { "featureType": "water", "elementType": "geometry", "stylers": [{"color": "#0e0f14"}] }
];

export default function MapScreen() {
  const [location, setLocation] = useState<Location.LocationObject | null>(null);
  const [friendsLocations, setFriendsLocations] = useState<Record<string, any>>({});
  const [usersMap, setUsersMap] = useState<Record<string, any>>({});
  const [userId, setUserId] = useState<string | null>(null);
  const [myFriends, setMyFriends] = useState<string[]>([]);

  // New features states
  const [currentCity, setCurrentCity] = useState("Locating...");
  const [currentRegion, setCurrentRegion] = useState("...");
  const [mapType, setMapType] = useState<'standard' | 'satellite'>('standard');
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [showMapMenu, setShowMapMenu] = useState(false);
  const [temperature, setTemperature] = useState("--");
  const [weatherIcon, setWeatherIcon] = useState<any>("cloud");
  const weatherCache = useRef<Record<string, { temp: string, icon: string, timestamp: number }>>({});

  useEffect(() => {
    const currentUser = auth.currentUser;
    if (!currentUser) return;
    setUserId(currentUser.uid);

    const userRef = doc(db, 'users', currentUser.uid);
    let currentGhostMode = 'precise';
    
    const unsubUser = onSnapshot(userRef, (docSnap) => {
      if (docSnap.exists()) {
        const data = docSnap.data();
        setMyFriends(data.friends || []);
        currentGhostMode = data.ghost_mode || 'precise';
      }
    });

    const unsubUsers = onSnapshot(collection(db, 'users'), (snapshot) => {
      const uMap: Record<string, any> = {};
      snapshot.forEach((doc) => {
        uMap[doc.id] = doc.data();
      });
      setUsersMap(uMap);
    });

    (async () => {
      let { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') return;

      let loc = await Location.getCurrentPositionAsync({});
      setLocation(loc);

      await Location.watchPositionAsync(
        { timeInterval: 3000, distanceInterval: 1 }, 
        async (newLoc) => {
          setLocation(newLoc);
          if (currentGhostMode === 'frozen') return; 

          let lat = newLoc.coords.latitude;
          let lng = newLoc.coords.longitude;

          if (currentGhostMode === 'blurred') {
            lat += (Math.random() - 0.5) * 0.02;
            lng += (Math.random() - 0.5) * 0.02;
          }

          const batteryLevel = await Battery.getBatteryLevelAsync();
          const batteryState = await Battery.getBatteryStateAsync();
          const isCharging = batteryState === Battery.BatteryState.CHARGING;

          await setDoc(doc(db, 'locations', currentUser.uid), {
            user_id: currentUser.uid,
            latitude: lat,
            longitude: lng,
            battery_level: Math.round((batteryLevel || 1) * 100),
            is_charging: isCharging,
            updated_at: new Date().toISOString(),
          });
        }
      );
    })();

    const unsubLocations = onSnapshot(collection(db, 'locations'), (snapshot) => {
      const locMap: Record<string, any> = {};
      snapshot.forEach((doc) => {
        locMap[doc.id] = doc.data();
      });
      setFriendsLocations(locMap);
    });

    return () => {
      unsubUser();
      unsubUsers();
      unsubLocations();
    };
  }, []);

  // Reverse Geocoding when the user pans the map
  const handleRegionChange = async (region: any) => {
    try {
      const geo = await Location.reverseGeocodeAsync({ latitude: region.latitude, longitude: region.longitude });
      if (geo.length > 0) {
        const place = geo[0];
        setCurrentCity(place.subregion || place.city || place.name || "Unknown");
        setCurrentRegion(place.region || place.country || "Area");
      }

      // Weather Fetching with 6-hour Cache
      const cacheKey = `${region.latitude.toFixed(1)},${region.longitude.toFixed(1)}`;
      const now = Date.now();
      const cached = weatherCache.current[cacheKey];

      // Use cache if less than 6 hours old (21600000 ms)
      if (cached && (now - cached.timestamp < 21600000)) {
        setTemperature(cached.temp);
        setWeatherIcon(cached.icon);
        return;
      }

      const weatherRes = await fetch(`https://api.open-meteo.com/v1/forecast?latitude=${region.latitude}&longitude=${region.longitude}&current_weather=true`);
      const weatherData = await weatherRes.json();
      
      if (weatherData && weatherData.current_weather) {
        const temp = `${weatherData.current_weather.temperature}°C`;
        const code = weatherData.current_weather.weathercode;
        
        let icon = 'cloud';
        if (code === 0) icon = 'sunny';
        else if (code <= 3) icon = 'partly-sunny';
        else if (code <= 48) icon = 'cloud';
        else if (code <= 67) icon = 'rainy';
        else if (code <= 77) icon = 'snow';
        else icon = 'thunderstorm';

        setTemperature(temp);
        setWeatherIcon(icon);
        weatherCache.current[cacheKey] = { temp, icon, timestamp: now };
      }
    } catch(e) {
      console.log("Geocode/Weather error", e);
    }
  };

  if (!location) {
    return (
      <View style={[styles.container, { justifyContent: 'center', alignItems: 'center' }]}>
        <ActivityIndicator size="large" color="#4285F4" />
        <Text style={{ color: '#333', marginTop: 10 }}>Finding you...</Text>
      </View>
    );
  }

  // Get the first 3 friends to display in the invite card
  const friendsToDisplay = myFriends.slice(0, 3);
  const emptySlots = Math.max(0, 3 - friendsToDisplay.length);

  return (
    <View style={styles.container}>
      <MapView
        provider={PROVIDER_DEFAULT}
        style={styles.map}
        initialRegion={{
          latitude: location.coords.latitude,
          longitude: location.coords.longitude,
          latitudeDelta: 0.05,
          longitudeDelta: 0.05,
        }}
        mapType={mapType}
        customMapStyle={isDarkMode ? darkMapStyle : []}
        showsUserLocation={false} 
        showsMyLocationButton={false}
        showsCompass={false}
        onRegionChangeComplete={handleRegionChange} // Trigger reverse geocoding on drag!
      >
        {/* Render Ourselves */}
        {location && (
           <Marker
             coordinate={{ latitude: location.coords.latitude, longitude: location.coords.longitude }}
             anchor={{ x: 0.5, y: 1 }}
             tracksViewChanges={true}
           >
             <View style={styles.markerWrapper}>
               <LinearGradient
                 colors={['#FF3B30', '#FF9500', '#FFCC00', '#4CD964', '#5AC8FA', '#007AFF', '#5856D6']}
                 start={{x: 0, y: 0}}
                 end={{x: 1, y: 1}}
                 style={styles.markerCircle}
               >
                 <Text style={styles.markerInitials}>me</Text>
               </LinearGradient>
               <View style={styles.markerPointer} />
             </View>
           </Marker>
        )}

        {/* Render Friends */}
        {Object.values(friendsLocations).map((friendLoc) => {
          if (friendLoc.user_id === userId) return null; 
          
          const friendProfile = usersMap[friendLoc.user_id] || {};
          const emailPrefix = friendProfile.email ? friendProfile.email.split('@')[0].substring(0, 2).toLowerCase() : 'tt';

          return (
            <Marker
              key={friendLoc.user_id}
              coordinate={{ latitude: friendLoc.latitude, longitude: friendLoc.longitude }}
              anchor={{ x: 0.5, y: 1 }}
              tracksViewChanges={true}
            >
              <View style={styles.markerWrapper}>
                <LinearGradient
                  colors={['#FF3B30', '#FF9500', '#FFCC00', '#4CD964', '#5AC8FA', '#007AFF', '#5856D6']}
                  start={{x: 0, y: 0}}
                  end={{x: 1, y: 1}}
                  style={styles.markerCircle}
                >
                  <Text style={styles.markerInitials}>{emailPrefix}</Text>
                </LinearGradient>
                <View style={styles.markerPointer} />
                <View style={styles.batteryPill}>
                  <Text style={styles.batteryText}>Just</Text>
                  <Ionicons name={friendLoc.is_charging ? "battery-charging" : "battery-half"} size={10} color="#34C759" style={{marginHorizontal: 2}} />
                  <Text style={styles.batteryText}>{friendLoc.battery_level}%</Text>
                </View>
              </View>
            </Marker>
          );
        })}
      </MapView>

      {/* TOP LEFT TEXT (Dynamic Geocoding) */}
      <View style={styles.topLeftContainer}>
        <Text style={[styles.districtText, isDarkMode && {color: '#fff', textShadowColor: '#000'}]}>{currentCity}</Text>
        <Text style={[styles.districtText, isDarkMode && {color: '#fff', textShadowColor: '#000'}]}>{currentRegion}</Text>
        <View style={styles.weatherBadge}>
          <Ionicons name={weatherIcon} size={16} color="#333" />
          <Text style={styles.weatherText}> {temperature}</Text>
        </View>
      </View>

      {/* TOP RIGHT ICONS */}
      <View style={styles.topRightContainer}>
        <TouchableOpacity style={styles.roundBtnWhite}>
          <Ionicons name="add" size={26} color="#666" />
        </TouchableOpacity>
        <TouchableOpacity style={styles.roundBtnDark} onPress={() => router.push('/profile')}>
          <Text style={styles.roundBtnText}>tt</Text>
        </TouchableOpacity>
      </View>

      {/* RIGHT STACK ICONS (Layers & Crown) */}
      <View style={styles.rightStack}>
        <TouchableOpacity style={styles.stackBtnPink} onPress={() => setShowMapMenu(!showMapMenu)}>
          <Ionicons name="layers" size={20} color="#fff" />
        </TouchableOpacity>
        
        {/* Map Type Toggle Menu */}
        {showMapMenu && (
          <View style={styles.mapMenu}>
            <TouchableOpacity style={styles.mapMenuItem} onPress={() => {setMapType('standard'); setIsDarkMode(false); setShowMapMenu(false);}}>
              <Ionicons name="map" size={16} color="#fff" />
              <Text style={styles.mapMenuText}>Light</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.mapMenuItem} onPress={() => {setMapType('standard'); setIsDarkMode(true); setShowMapMenu(false);}}>
              <Ionicons name="moon" size={16} color="#fff" />
              <Text style={styles.mapMenuText}>Dark</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.mapMenuItem} onPress={() => {setMapType('satellite'); setShowMapMenu(false);}}>
              <Ionicons name="earth" size={16} color="#fff" />
              <Text style={styles.mapMenuText}>Satellite</Text>
            </TouchableOpacity>
          </View>
        )}

        <TouchableOpacity style={styles.stackBtnGradient}>
          <MaterialCommunityIcons name="crown" size={24} color="#fff" />
        </TouchableOpacity>
      </View>

      {/* BOTTOM INVITE CARD */}
      <View style={styles.inviteCard}>
        <View style={styles.inviteContent}>
          <Text style={styles.inviteTitle}>Invite</Text>
          <Text style={styles.inviteDesc}>Map your world with more friends</Text>
          <View style={styles.inviteBtnRow}>
            
            {/* 1. Show existing friends avatars first */}
            {friendsToDisplay.map((friendId) => {
               const p = usersMap[friendId];
               const prefix = p?.email ? p.email.substring(0, 2).toUpperCase() : 'TT';
               return (
                 <View key={friendId} style={styles.inviteFriendBtn}>
                    <Text style={styles.inviteFriendText}>{prefix}</Text>
                 </View>
               );
            })}
            
            {/* 2. Show remaining '+' buttons that route to profile */}
            {Array.from({ length: emptySlots }).map((_, i) => (
              <TouchableOpacity key={i} style={styles.inviteAddBtn} onPress={() => router.push('/profile')}>
                <Ionicons name="add" size={22} color="#888" />
              </TouchableOpacity>
            ))}

          </View>
        </View>
        <Text style={styles.hugeEmoji}>😘</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f0f0f5' },
  map: { ...StyleSheet.absoluteFillObject },

  /* Top Left */
  topLeftContainer: { position: 'absolute', top: 50, left: 20 },
  districtText: {
    fontSize: 32, fontWeight: '900', color: '#000',
    textShadowColor: '#fff', textShadowOffset: { width: 1, height: 1 }, textShadowRadius: 10,
    letterSpacing: -1,
  },
  weatherBadge: {
    flexDirection: 'row', alignItems: 'center', marginTop: 8,
    backgroundColor: '#fff', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 20, alignSelf: 'flex-start',
    shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 4, elevation: 2
  },
  weatherText: { fontSize: 14, fontWeight: 'bold', color: '#333' },

  /* Top Right */
  topRightContainer: { position: 'absolute', top: 50, right: 20, flexDirection: 'row', alignItems: 'center' },
  roundBtnWhite: {
    width: 44, height: 44, borderRadius: 22, backgroundColor: '#fff', alignItems: 'center', justifyContent: 'center',
    marginRight: 10, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 4, elevation: 2
  },
  roundBtnDark: {
    width: 44, height: 44, borderRadius: 22, backgroundColor: '#444', alignItems: 'center', justifyContent: 'center',
    shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.2, shadowRadius: 4, elevation: 2
  },
  roundBtnText: { color: '#fff', fontSize: 18, fontWeight: 'bold' },

  /* Right Stack */
  rightStack: { position: 'absolute', top: 120, right: 20, gap: 15, alignItems: 'flex-end' },
  stackBtnPink: {
    width: 40, height: 40, borderRadius: 10, backgroundColor: '#FF6B9D', alignItems: 'center', justifyContent: 'center',
    transform: [{ rotate: '10deg' }], shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.2, shadowRadius: 4, elevation: 3
  },
  stackBtnGradient: {
    width: 40, height: 40, borderRadius: 10, backgroundColor: '#FFB547', alignItems: 'center', justifyContent: 'center',
    transform: [{ rotate: '-5deg' }], shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.2, shadowRadius: 4, elevation: 3
  },
  
  /* Map Menu */
  mapMenu: {
    backgroundColor: 'rgba(0,0,0,0.8)', borderRadius: 10, padding: 10,
    position: 'absolute', right: 50, top: 0, width: 100, gap: 10
  },
  mapMenuItem: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  mapMenuText: { color: '#fff', fontSize: 12, fontWeight: 'bold' },

  /* Bottom Invite Card */
  inviteCard: {
    position: 'absolute', bottom: 30, left: 20, right: 20,
    backgroundColor: '#000', borderRadius: 24, padding: 20,
    flexDirection: 'row', alignItems: 'center',
    shadowColor: '#000', shadowOffset: { width: 0, height: 10 }, shadowOpacity: 0.3, shadowRadius: 20, elevation: 10
  },
  inviteContent: { flex: 1 },
  inviteTitle: { color: '#fff', fontSize: 20, fontWeight: 'bold', marginBottom: 4 },
  inviteDesc: { color: '#888', fontSize: 13, marginBottom: 16 },
  inviteBtnRow: { flexDirection: 'row', gap: 10 },
  inviteAddBtn: {
    width: 36, height: 36, borderRadius: 18, backgroundColor: '#222', alignItems: 'center', justifyContent: 'center'
  },
  inviteFriendBtn: {
    width: 36, height: 36, borderRadius: 18, backgroundColor: '#FFCA28', alignItems: 'center', justifyContent: 'center'
  },
  inviteFriendText: { color: '#000', fontWeight: 'bold', fontSize: 12 },
  emojiContainer: { justifyContent: 'center', alignItems: 'center', marginLeft: 10 },
  hugeEmoji: { fontSize: 60 },

  /* Marker */
  markerWrapper: { alignItems: 'center', width: 100, height: 100, justifyContent: 'flex-end', overflow: 'visible' },
  markerCircle: {
    width: 50, height: 50, borderRadius: 25,
    alignItems: 'center', justifyContent: 'center', borderWidth: 2, borderColor: '#fff'
  },
  markerInitials: { color: '#fff', fontSize: 20, fontWeight: 'bold', textShadowColor: 'rgba(0,0,0,0.3)', textShadowOffset: {width: 1, height: 1}, textShadowRadius: 2 },
  markerPointer: {
    width: 0, height: 0, borderLeftWidth: 6, borderRightWidth: 6, borderTopWidth: 8,
    borderStyle: 'solid', backgroundColor: 'transparent',
    borderLeftColor: 'transparent', borderRightColor: 'transparent', borderTopColor: '#fff',
    marginBottom: 4
  },
  batteryPill: {
    flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff',
    paddingHorizontal: 8, paddingVertical: 4, borderRadius: 12,
    shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.2, shadowRadius: 4, elevation: 3
  },
  batteryText: { fontSize: 10, fontWeight: 'bold', color: '#000' }
});
