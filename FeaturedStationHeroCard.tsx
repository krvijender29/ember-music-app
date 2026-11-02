import React, { useState, useEffect, useRef, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Pressable,
  Animated,
  Platform,
  Vibration,
  ViewStyle,
} from 'react-native';
import LinearGradient from 'react-native-linear-gradient';

// ============================================================================
// 1. TYPES & INTERFACES
// ============================================================================

export interface Track {
  id: string;
  title: string;
  artist: string;
  thumbnail: string;
  duration?: string;
  streamUrl?: string;
  isLiked?: boolean;
}

export interface FeaturedStationHeroCardProps {
  /** Tracks belonging to this featured station playlist */
  tracks: Track[];
  /** First name of the authenticated user (e.g. 'Vijender') */
  userFirstName?: string;
  /** User's top listened artist (e.g. 'Arijit Singh') */
  topArtist?: string;
  /** Fallback 2 curated image URL when history is empty */
  defaultCuratedBanner?: string;
  /** Callback when user taps "Play Now" */
  onPlayStation?: (tracks: Track[]) => void;
  /** Callback when user taps "Add to Queue" */
  onAddToQueue?: (tracks: Track[]) => void;
  /** Optional container style overrides */
  style?: ViewStyle;
}

// Default curated fallback banner if no history exists
const DEFAULT_BANNER =
  'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=1200&auto=format&fit=crop';

// ============================================================================
// 2. LISTENING HISTORY STATE HOOK
// ============================================================================

/**
 * Custom hook managing the user's listening history and computing the
 * 2nd-last played song cover thumbnail with fallbacks.
 */
export function useListeningHistory(initialHistory: Track[] = [], defaultBanner = DEFAULT_BANNER) {
  const [history, setHistory] = useState<Track[]>(initialHistory);

  // Record a track into history (adds to front, de-duplicates, caps at 50)
  const recordTrack = (track: Track) => {
    setHistory((prev) => {
      const filtered = prev.filter((t) => t.id !== track.id);
      return [track, ...filtered].slice(0, 50);
    });
  };

  // Clear history
  const clearHistory = () => setHistory([]);

  /**
   * Hero Cover Resolution Logic:
   * 1. 2nd-last played song: `history[1]?.thumbnail`
   * 2. Fallback 1: If history has only 1 track -> `history[0]?.thumbnail`
   * 3. Fallback 2: If history is empty -> `defaultBanner`
   */
  const heroArtwork = useMemo(() => {
    if (history.length >= 2 && history[1]?.thumbnail) {
      return history[1].thumbnail;
    }
    if (history.length === 1 && history[0]?.thumbnail) {
      return history[0].thumbnail;
    }
    return defaultBanner;
  }, [history, defaultBanner]);

  return {
    history,
    heroArtwork,
    recordTrack,
    clearHistory,
  };
}

// ============================================================================
// 3. SPRING BUTTON COMPONENT (TACTILE PRESS FEEDBACK)
// ============================================================================

interface SpringButtonProps {
  onPress: () => void;
  children: React.ReactNode;
  style?: ViewStyle | ViewStyle[];
}

const SpringButton: React.FC<SpringButtonProps> = ({ onPress, children, style }) => {
  const scaleAnim = useRef(new Animated.Value(1)).current;

  const handlePressIn = () => {
    Animated.spring(scaleAnim, {
      toValue: 0.93,
      useNativeDriver: true,
      friction: 5,
      tension: 100,
    }).start();
  };

  const handlePressOut = () => {
    Animated.spring(scaleAnim, {
      toValue: 1,
      useNativeDriver: true,
      friction: 4,
      tension: 80,
    }).start();
  };

  const handlePress = () => {
    if (Platform.OS === 'android') {
      Vibration.vibrate(12);
    }
    onPress();
  };

  return (
    <Pressable
      onPress={handlePress}
      onPressIn={handlePressIn}
      onPressOut={handlePressOut}
      android_ripple={{ color: 'rgba(255,255,255,0.12)', borderless: false }}
    >
      <Animated.View style={[style, { transform: [{ scale: scaleAnim }] }]}>
        {children}
      </Animated.View>
    </Pressable>
  );
};

// ============================================================================
// 4. MAIN FEATURED STATION HERO CARD
// ============================================================================

export const FeaturedStationHeroCard: React.FC<FeaturedStationHeroCardProps> = ({
  tracks,
  userFirstName,
  topArtist,
  defaultCuratedBanner = DEFAULT_BANNER,
  onPlayStation,
  onAddToQueue,
  style,
}) => {
  // Listening history hook
  const { heroArtwork } = useListeningHistory(tracks, defaultCuratedBanner);

  // Image crossfade state
  const [currentImage, setCurrentImage] = useState<string>(heroArtwork);
  const [previousImage, setPreviousImage] = useState<string | null>(null);
  const crossfadeAnim = useRef(new Animated.Value(1)).current;

  // Trigger 400ms crossfade whenever the resolved 2nd-last artwork changes
  useEffect(() => {
    if (heroArtwork !== currentImage) {
      setPreviousImage(currentImage);
      setCurrentImage(heroArtwork);
      crossfadeAnim.setValue(0);

      Animated.timing(crossfadeAnim, {
        toValue: 1,
        duration: 400,
        useNativeDriver: true,
      }).start(() => {
        setPreviousImage(null);
      });
    }
  }, [heroArtwork]);

  // Dynamic Greeting based on current local hour
  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good Morning';
    if (hour < 18) return 'Good Afternoon';
    return 'Good Evening';
  }, []);

  const displayName = userFirstName?.trim() || 'Explorer';

  // Contextual Subtitle
  const subtitle = useMemo(() => {
    if (topArtist?.trim()) {
      return `Vibing with ${topArtist.trim()} and curated soundscapes tailored for you.`;
    }
    return 'Immerse in pure 320kbps high-fidelity soundscapes tailored for you.';
  }, [topArtist]);

  return (
    <View style={[styles.cardContainer, style]}>
      {/* ------------------------------------------------------------------ */}
      {/* A. DYNAMIC BACKGROUND IMAGE WITH 400MS CROSSFADE                  */}
      {/* ------------------------------------------------------------------ */}
      {previousImage && (
        <Animated.Image
          source={{ uri: previousImage }}
          style={[styles.backgroundImage, { opacity: 1 }]}
          resizeMode="cover"
          blurRadius={Platform.OS === 'ios' ? 18 : 12}
        />
      )}
      <Animated.Image
        source={{ uri: currentImage }}
        style={[
          styles.backgroundImage,
          {
            opacity: previousImage
              ? crossfadeAnim.interpolate({
                  inputRange: [0, 1],
                  outputRange: [0, 0.45],
                })
              : 0.45,
          },
        ]}
        resizeMode="cover"
        blurRadius={Platform.OS === 'ios' ? 18 : 12}
      />

      {/* ------------------------------------------------------------------ */}
      {/* B. DUAL SCRIM OVERLAYS                                             */}
      {/* ------------------------------------------------------------------ */}
      {/* 1. Horizontal Scrim: Left-to-right gradient for crisp readability */}
      <LinearGradient
        colors={['rgba(9, 9, 9, 0.95)', 'rgba(9, 9, 9, 0.75)', 'rgba(9, 9, 9, 0.25)']}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 0 }}
        style={StyleSheet.absoluteFillObject}
      />

      {/* 2. Ambient Glow: Top-right subtle radial glow in #FF5A36 (20% opacity) */}
      <View style={styles.topRightAmbientGlow} />

      {/* ------------------------------------------------------------------ */}
      {/* C. CARD CONTENT & EDITORIAL BADGE                                  */}
      {/* ------------------------------------------------------------------ */}
      <View style={styles.contentContainer}>
        {/* Editorial Tag Badge */}
        <View style={styles.badgeRow}>
          <View style={styles.glowingDot} />
          <Text style={styles.badgeText}>FEATURED STATION</Text>
        </View>

        {/* Dynamic Greeting Title */}
        <Text style={styles.greetingTitle} numberOfLines={1}>
          {greeting}, {displayName}
        </Text>

        {/* Contextual Subtitle */}
        <Text style={styles.subtitle} numberOfLines={2}>
          {subtitle}
        </Text>

        {/* ------------------------------------------------------------------ */}
        {/* D. ACTION BUTTONS ROW (CAPSULES)                                   */}
        {/* ------------------------------------------------------------------ */}
        <View style={styles.actionButtonsRow}>
          {/* Play Now Button */}
          <SpringButton
            onPress={() => onPlayStation?.(tracks)}
            style={styles.playNowButton}
          >
            <View style={styles.playIconTriangle} />
            <Text style={styles.playNowText}>Play Now</Text>
          </SpringButton>

          {/* Add to Queue Button */}
          <SpringButton
            onPress={() => onAddToQueue?.(tracks)}
            style={styles.addToQueueButton}
          >
            <View style={styles.queueIcon}>
              <View style={styles.queueBar} />
              <View style={styles.queueBar} />
              <View style={styles.queueBar} />
            </View>
            <Text style={styles.addToQueueText}>Add to Queue</Text>
          </SpringButton>
        </View>
      </View>
    </View>
  );
};

// ============================================================================
// 5. STYLES (EXACT TOKENS & SPECS)
// ============================================================================

const styles = StyleSheet.create({
  cardContainer: {
    width: '100%',
    minHeight: 175,
    borderRadius: 22, // 22dp squircle
    backgroundColor: '#111111',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.10)',
    overflow: 'hidden',
    position: 'relative',
    justifyContent: 'flex-end',
    // Shadow: 0 12px 35px rgba(0, 0, 0, 0.70)
    ...Platform.select({
      ios: {
        shadowColor: '#000000',
        shadowOffset: { width: 0, height: 12 },
        shadowOpacity: 0.7,
        shadowRadius: 18,
      },
      android: {
        elevation: 10,
      },
    }),
  },
  backgroundImage: {
    ...StyleSheet.absoluteFillObject,
    width: '100%',
    height: '100%',
  },
  topRightAmbientGlow: {
    position: 'absolute',
    top: -40,
    right: -40,
    width: 180,
    height: 180,
    borderRadius: 90,
    backgroundColor: 'rgba(255, 90, 54, 0.20)', // #FF5A36 20%
    // Soft radial blur simulation
    ...Platform.select({
      ios: {
        shadowColor: '#FF5A36',
        shadowOffset: { width: 0, height: 0 },
        shadowOpacity: 0.8,
        shadowRadius: 40,
      },
    }),
  },
  contentContainer: {
    padding: 18,
    zIndex: 2,
  },
  badgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
  },
  glowingDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#FF5A36',
    marginRight: 7,
    // Glowing Dot box-shadow: 0 0 8px #FF5A36
    ...Platform.select({
      ios: {
        shadowColor: '#FF5A36',
        shadowOffset: { width: 0, height: 0 },
        shadowOpacity: 1,
        shadowRadius: 4,
      },
      android: {
        elevation: 3,
      },
    }),
  },
  badgeText: {
    fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace',
    fontSize: 10.5,
    fontWeight: '700',
    letterSpacing: 1.2,
    color: '#FF5A36',
    textTransform: 'uppercase',
  },
  greetingTitle: {
    fontSize: 22,
    fontWeight: '800', // Extra Bold
    color: '#FFFFFF',
    letterSpacing: -0.4,
    marginBottom: 3,
    fontFamily: Platform.OS === 'ios' ? 'SF Pro Display' : 'sans-serif-black',
  },
  subtitle: {
    fontSize: 13,
    fontWeight: '400',
    color: '#A1A1AA', // Zinc-400
    lineHeight: 18,
    marginBottom: 14,
    maxWidth: '92%',
  },
  actionButtonsRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  playNowButton: {
    height: 38,
    borderRadius: 999, // Fully rounded capsule
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 18,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 10,
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.25,
        shadowRadius: 5,
      },
      android: {
        elevation: 4,
      },
    }),
  },
  playIconTriangle: {
    width: 0,
    height: 0,
    borderLeftWidth: 10,
    borderRightWidth: 0,
    borderTopWidth: 6,
    borderBottomWidth: 6,
    borderStyle: 'solid',
    backgroundColor: 'transparent',
    borderLeftColor: '#090909',
    borderTopColor: 'transparent',
    borderBottomColor: 'transparent',
    marginRight: 7,
  },
  playNowText: {
    color: '#090909',
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 0.1,
  },
  addToQueueButton: {
    height: 38,
    borderRadius: 999,
    backgroundColor: 'rgba(255, 255, 255, 0.10)', // Frosted acrylic
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.16)',
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  queueIcon: {
    width: 14,
    height: 11,
    justifyContent: 'space-between',
    marginRight: 7,
  },
  queueBar: {
    height: 2,
    backgroundColor: '#FFFFFF',
    borderRadius: 1,
  },
  addToQueueText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '600',
    letterSpacing: 0.1,
  },
});

export default FeaturedStationHeroCard;
