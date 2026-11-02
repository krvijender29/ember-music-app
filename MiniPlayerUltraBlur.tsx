import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  Image,
  TouchableOpacity,
  StyleSheet,
  Animated,
  Dimensions,
  Platform,
} from 'react-native';
import { BlurView } from 'expo-blur';
import { Play, Pause, SkipForward, Heart } from 'lucide-react-native';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

interface MiniPlayerUltraBlurProps {
  title: string;
  artist: string;
  thumbnail: string;
  isPlaying: boolean;
  isLiked: boolean;
  progressPercent: number; // 0 - 100
  bottomNavHeight?: number; // default ~64dp
  safeAreaBottom?: number;
  extractedColor?: string; // optional dynamically extracted color
  onPressCard: () => void;
  onTogglePlay: () => void;
  onNextTrack: () => void;
  onToggleLike: () => void;
}

/**
 * EMBER Music - Production-Grade Mobile Mini Player
 *
 * Visual Rules Enforced:
 * 1. CLEAN TOP & 100% FROSTED BLUR:
 *    - Completely removed any top color block/arch/halo above the player.
 *    - Top area is 100% clean obsidian black (#090909) with zero light bleeding onto song lists.
 *    - Ultra-sheer glass background: rgba(18, 18, 24, 0.60) with BlurView intensity 95.
 * 2. SIDE-ONLY AMBIENT GLOW:
 *    - Soft, subtle ambient glow radiating ONLY from LEFT and RIGHT flanks.
 *    - borderLeftWidth: 1.8, borderRightWidth: 1.8 in thumbnailColor.
 *    - shadowOffset: { width: 0, height: 0 }, shadowOpacity: 0.38, shadowRadius: 20 (zero vertical glow).
 * 3. EXACT WEBSITE GEOMETRY & SIGNATURE CONTROLS:
 *    - Height 60dp, left 8dp, right 8dp, radius 14dp squircle.
 *    - 44x44dp thumbnail, 36x36dp crisp white circular play button with dark #090909 icon.
 *    - Bottom 2dp micro-progress line with thumbnailColor fill.
 */
export const MiniPlayerUltraBlur: React.FC<MiniPlayerUltraBlurProps> = ({
  title,
  artist,
  thumbnail,
  isPlaying,
  isLiked,
  progressPercent = 0,
  bottomNavHeight = 64,
  safeAreaBottom = 0,
  extractedColor,
  onPressCard,
  onTogglePlay,
  onNextTrack,
  onToggleLike,
}) => {
  // Active thumbnail color with fallback to Ember Coral Amber #FF5A36
  const activeColor = extractedColor || '#FF5A36';

  // Animated color transition
  const colorAnim = useRef(new Animated.Value(0)).current;
  const currentColorRef = useRef(activeColor);

  useEffect(() => {
    currentColorRef.current = activeColor;
    Animated.timing(colorAnim, {
      toValue: 1,
      duration: 500,
      useNativeDriver: false,
    }).start(() => {
      colorAnim.setValue(0);
    });
  }, [activeColor]);

  // Touch press feedback for Play button
  const playScaleAnim = useRef(new Animated.Value(1)).current;
  const handlePlayPressIn = () => {
    Animated.spring(playScaleAnim, {
      toValue: 0.92,
      useNativeDriver: true,
      speed: 50,
      bounciness: 0,
    }).start();
  };
  const handlePlayPressOut = () => {
    Animated.spring(playScaleAnim, {
      toValue: 1,
      useNativeDriver: true,
      speed: 35,
      bounciness: 8,
    }).start();
  };

  // Like button pulse animation
  const likeScaleAnim = useRef(new Animated.Value(1)).current;
  const handleLikePress = () => {
    Animated.sequence([
      Animated.timing(likeScaleAnim, { toValue: 1.3, duration: 120, useNativeDriver: true }),
      Animated.spring(likeScaleAnim, { toValue: 1, friction: 4, useNativeDriver: true }),
    ]).start();
    onToggleLike();
  };

  const bottomPosition = bottomNavHeight + safeAreaBottom + 6;

  return (
    <View
      style={[
        styles.islandContainer,
        {
          bottom: bottomPosition,
          // SIDE-ONLY AMBIENT GLOW:
          // Zero vertical offset prevents upward/downward bleed
          shadowColor: activeColor,
          shadowOffset: { width: 0, height: 0 },
          shadowOpacity: 0.38,
          shadowRadius: 20,
          elevation: 8,
          borderLeftColor: activeColor,
          borderRightColor: activeColor,
        },
      ]}
    >
      {/* 1. TRUE FROSTED GLASS SURFACE */}
      <BlurView
        intensity={95}
        tint="dark"
        experimentalBlurMethod="dimezisBlurView"
        style={StyleSheet.absoluteFillObject}
      />

      {/* Ultra-sheer glass tint allowing backdrop blur to shine through */}
      <View style={styles.sheerGlassTint} />

      {/* Top and Bottom hairline borders */}
      <View style={styles.topHairline} />
      <View style={styles.bottomHairline} />

      {/* 2. MAIN CONTENT (Padding: 0 12dp) */}
      <TouchableOpacity
        activeOpacity={0.92}
        onPress={onPressCard}
        style={styles.cardContentWrapper}
      >
        {/* Left Section: 44x44dp Squircle Thumbnail & Track Meta */}
        <View style={styles.leftSection}>
          <View style={styles.thumbnailFrame}>
            <Image
              source={{ uri: thumbnail }}
              style={styles.thumbnailImage}
              resizeMode="cover"
            />
          </View>

          <View style={styles.textCluster}>
            <Text numberOfLines={1} style={styles.titleText}>
              {title}
            </Text>
            <Text numberOfLines={1} style={styles.artistText}>
              {artist}
            </Text>
          </View>
        </View>

        {/* Right Section: Like, Play/Pause Circle, Next */}
        <View style={styles.rightControlsCluster}>
          {/* Like Heart Button */}
          <TouchableOpacity
            activeOpacity={0.7}
            onPress={handleLikePress}
            style={styles.iconHitArea}
            hitSlop={{ top: 10, bottom: 10, left: 6, right: 6 }}
          >
            <Animated.View style={{ transform: [{ scale: likeScaleAnim }] }}>
              <Heart
                size={20}
                color={isLiked ? activeColor : '#FFFFFF'}
                fill={isLiked ? activeColor : 'none'}
              />
            </Animated.View>
          </TouchableOpacity>

          {/* Solid Pure White Play Circle (36x36dp) */}
          <Animated.View style={{ transform: [{ scale: playScaleAnim }] }}>
            <TouchableOpacity
              activeOpacity={0.9}
              onPressIn={handlePlayPressIn}
              onPressOut={handlePlayPressOut}
              onPress={onTogglePlay}
              style={styles.playPauseButton}
            >
              {isPlaying ? (
                <Pause size={18} color="#090909" fill="#090909" />
              ) : (
                <Play
                  size={18}
                  color="#090909"
                  fill="#090909"
                  style={{ marginLeft: 2 }}
                />
              )}
            </TouchableOpacity>
          </Animated.View>

          {/* Next Button */}
          <TouchableOpacity
            activeOpacity={0.7}
            onPress={onNextTrack}
            style={styles.iconHitArea}
            hitSlop={{ top: 10, bottom: 10, left: 6, right: 10 }}
          >
            <SkipForward size={20} color="#FFFFFF" fill="#FFFFFF" />
          </TouchableOpacity>
        </View>
      </TouchableOpacity>

      {/* 3. BOTTOM MICRO-PROGRESS LINE */}
      <View style={styles.progressContainer}>
        {/* Progress Fill */}
        <View
          style={[
            styles.progressFill,
            {
              width: `${Math.min(Math.max(progressPercent, 0), 100)}%`,
              backgroundColor: activeColor,
              shadowColor: activeColor,
            },
          ]}
        />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  islandContainer: {
    position: 'absolute',
    left: 8,
    right: 8,
    height: 60,
    borderRadius: 14,
    overflow: 'hidden',
    backgroundColor: 'rgba(18, 18, 24, 0.60)',
    // Lateral side borders (Left & Right flanks get active color)
    borderLeftWidth: 1.8,
    borderRightWidth: 1.8,
    // Top & bottom subtle bevels
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.12)',
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.08)',
  },
  sheerGlassTint: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(18, 18, 24, 0.35)',
  },
  topHairline: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 1,
    backgroundColor: 'rgba(255, 255, 255, 0.12)',
  },
  bottomHairline: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: 1,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
  },
  cardContentWrapper: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 12,
  },
  leftSection: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    marginRight: 10,
  },
  thumbnailFrame: {
    width: 44,
    height: 44,
    borderRadius: 10,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.12)',
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 10,
    elevation: 4,
    backgroundColor: '#181818',
  },
  thumbnailImage: {
    width: '100%',
    height: '100%',
  },
  textCluster: {
    flex: 1,
    marginLeft: 10,
    justifyContent: 'center',
  },
  titleText: {
    fontSize: 13.5,
    fontWeight: '600',
    color: '#FFFFFF',
    letterSpacing: -0.2,
  },
  artistText: {
    fontSize: 11.5,
    fontWeight: '400',
    color: '#A1A1AA',
    marginTop: 1.5,
  },
  rightControlsCluster: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  iconHitArea: {
    width: 32,
    height: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  playPauseButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#FFFFFF',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 10,
    elevation: 4,
  },
  progressContainer: {
    position: 'absolute',
    bottom: 0,
    left: 10,
    right: 10,
    height: 2,
    borderRadius: 999,
    backgroundColor: 'rgba(255, 255, 255, 0.10)',
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    borderRadius: 999,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.7,
    shadowRadius: 6,
  },
});

export default MiniPlayerUltraBlur;
