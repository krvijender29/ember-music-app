import React, { useRef, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  Image,
  Pressable,
  Animated,
  Platform,
  Vibration,
  ViewStyle,
} from 'react-native';
import LinearGradient from 'react-native-linear-gradient';

// ============================================================================
// 1. DATA MODELS & TYPES
// ============================================================================

export interface Track {
  id: string;
  title: string;
  artist: string;
  thumbnail: string;
  duration: string;
  streamUrl?: string;
  isLiked?: boolean;
}

export interface Playlist {
  id: string;
  title: string;
  coverUrl?: string;
  songCount: number;
  creatorName?: string;
  tracks: Track[];
}

export interface PlaybackState {
  currentTrackId: string | null;
  isPlaying: boolean;
}

export interface HomeScreenSongSectionsProps {
  /** Section 1: User's recently played tracks */
  jumpBackInTracks: Track[];
  /** Section 2: Global top charts ranked tracks */
  topChartsTracks: Track[];
  /** Top listened artist name (e.g. 'Arijit Singh') */
  topArtistName?: string;
  /** Section 3: Recommended tracks from the user's top artist */
  topArtistTracks?: Track[];
  /** Section 4: Algorithmically recommended tracks based on preferences */
  tasteRecommendationTracks: Track[];
  /** Section 5: Daily Mix curated tracks */
  dailyMixTracks: Track[];
  /** Section 6: User created/saved playlists */
  userPlaylists: Playlist[];
  /** Current audio playback status */
  playbackState: PlaybackState;
  /** Primary queue play handler: plays track and queues remainder */
  onPlayTrack: (track: Track, newQueue: Track[]) => void;
  /** Play all button handler for a section */
  onPlayAll: (sectionName: string, tracks: Track[]) => void;
  /** Toggle favorite / like on a track */
  onToggleLikeTrack?: (track: Track) => void;
  /** 3-dots more options bottom sheet trigger */
  onOpenTrackOptions?: (track: Track) => void;
  /** Section 4 pill trigger: opens preferences modal */
  onTuneTastePress?: () => void;
  /** Section 6 item trigger: opens Opened Playlist View */
  onOpenPlaylist?: (playlist: Playlist) => void;
  /** Section 6 header trigger: navigates to Library tab */
  onNavigateToLibrary?: () => void;
  /** Optional container style */
  style?: ViewStyle;
}

// ============================================================================
// 2. HELPER UI COMPONENTS
// ============================================================================

/** Tactile Spring Pressable for cards and buttons */
const SpringCard: React.FC<{
  onPress: () => void;
  children: React.ReactNode;
  style?: ViewStyle | ViewStyle[];
}> = ({ onPress, children, style }) => {
  const scale = useRef(new Animated.Value(1)).current;

  const onPressIn = () => {
    Animated.spring(scale, {
      toValue: 0.94,
      useNativeDriver: true,
      friction: 5,
      tension: 120,
    }).start();
  };

  const onPressOut = () => {
    Animated.spring(scale, {
      toValue: 1,
      useNativeDriver: true,
      friction: 4,
      tension: 80,
    }).start();
  };

  const handlePress = () => {
    if (Platform.OS === 'android') Vibration.vibrate(10);
    onPress();
  };

  return (
    <Pressable onPress={handlePress} onPressIn={onPressIn} onPressOut={onPressOut}>
      <Animated.View style={[style, { transform: [{ scale }] }]}>{children}</Animated.View>
    </Pressable>
  );
};

/** 3-Bar Animated Audio Equalizer Badge */
const AnimatedEqualizer: React.FC = () => {
  const bar1 = useRef(new Animated.Value(4)).current;
  const bar2 = useRef(new Animated.Value(12)).current;
  const bar3 = useRef(new Animated.Value(8)).current;

  useEffect(() => {
    const createAnim = (val: Animated.Value, min: number, max: number, duration: number) =>
      Animated.loop(
        Animated.sequence([
          Animated.timing(val, { toValue: max, duration, useNativeDriver: false }),
          Animated.timing(val, { toValue: min, duration, useNativeDriver: false }),
        ])
      );

    const a1 = createAnim(bar1, 4, 16, 320);
    const a2 = createAnim(bar2, 3, 15, 420);
    const a3 = createAnim(bar3, 5, 14, 360);

    a1.start();
    a2.start();
    a3.start();

    return () => {
      a1.stop();
      a2.stop();
      a3.stop();
    };
  }, []);

  return (
    <View style={styles.eqContainer}>
      <Animated.View style={[styles.eqBar, { height: bar1 }]} />
      <Animated.View style={[styles.eqBar, { height: bar2 }]} />
      <Animated.View style={[styles.eqBar, { height: bar3 }]} />
    </View>
  );
};

/** Section Header Row */
const SectionHeader: React.FC<{
  title: string;
  icon?: string;
  iconColor?: string;
  subtitle?: string;
  actionText?: string;
  actionColor?: string;
  onActionPress?: () => void;
  isPillAction?: boolean;
}> = ({
  title,
  icon,
  iconColor,
  subtitle,
  actionText,
  actionColor = '#A1A1AA',
  onActionPress,
  isPillAction,
}) => (
  <View style={styles.sectionHeaderContainer}>
    <View style={styles.headerLeftCol}>
      <View style={styles.headerTitleRow}>
        {icon && <Text style={[styles.headerIcon, iconColor ? { color: iconColor } : null]}>{icon} </Text>}
        <Text style={styles.headerTitle}>{title}</Text>
      </View>
      {subtitle && <Text style={styles.headerSubtitle}>{subtitle}</Text>}
    </View>
    {actionText && (
      <Pressable
        onPress={onActionPress}
        style={isPillAction ? styles.actionPill : styles.actionTextBtn}
        hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
      >
        <Text
          style={[
            isPillAction ? styles.actionPillText : styles.actionText,
            actionColor ? { color: actionColor } : null,
          ]}
        >
          {actionText}
        </Text>
      </Pressable>
    )}
  </View>
);

/** Standard Song Row Component (Used in Sections 3 & 4) */
const SongRow: React.FC<{
  track: Track;
  index: number;
  isActive: boolean;
  isPlaying: boolean;
  onPress: () => void;
  onToggleLike?: () => void;
  onOptionsPress?: () => void;
}> = ({ track, index, isActive, isPlaying, onPress, onToggleLike, onOptionsPress }) => (
  <Pressable
    onPress={onPress}
    style={({ pressed }) => [styles.songRow, pressed && styles.songRowPressed]}
  >
    {/* Index or Equalizer */}
    <View style={styles.songIndexCol}>
      {isActive && isPlaying ? (
        <AnimatedEqualizer />
      ) : (
        <Text style={[styles.songIndexText, isActive && styles.songIndexActive]}>
          {String(index + 1).padStart(2, '0')}
        </Text>
      )}
    </View>

    {/* Squircle Thumbnail */}
    <Image source={{ uri: track.thumbnail }} style={styles.songThumbnail} resizeMode="cover" />

    {/* Title & Artist */}
    <View style={styles.songMetaCol}>
      <Text
        style={[styles.songTitle, isActive && styles.songTitleActive]}
        numberOfLines={1}
        ellipsizeMode="end"
      >
        {track.title}
      </Text>
      <Text style={styles.songArtist} numberOfLines={1} ellipsizeMode="end">
        {track.artist}
      </Text>
    </View>

    {/* Timestamp Duration */}
    <Text style={styles.songDuration}>{track.duration}</Text>

    {/* Like Heart Button */}
    <Pressable
      onPress={onToggleLike}
      style={styles.songActionBtn}
      hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
    >
      <Text style={[styles.songLikeIcon, track.isLiked && styles.songLikeIconActive]}>
        {track.isLiked ? '♥' : '♡'}
      </Text>
    </Pressable>

    {/* 3-Dots More Options */}
    <Pressable
      onPress={onOptionsPress}
      style={styles.songActionBtn}
      hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
    >
      <Text style={styles.songOptionsIcon}>⋮</Text>
    </Pressable>
  </Pressable>
);

// ============================================================================
// 3. MAIN COMPONENT: ALL 6 SONG SECTIONS
// ============================================================================

export const HomeScreenSongSections: React.FC<HomeScreenSongSectionsProps> = ({
  jumpBackInTracks,
  topChartsTracks,
  topArtistName,
  topArtistTracks,
  tasteRecommendationTracks,
  dailyMixTracks,
  userPlaylists,
  playbackState,
  onPlayTrack,
  onPlayAll,
  onToggleLikeTrack,
  onOpenTrackOptions,
  onTuneTastePress,
  onOpenPlaylist,
  onNavigateToLibrary,
  style,
}) => {
  // Helper to play a track and queue remaining items in a list
  const handlePlayFromList = (clicked: Track, list: Track[]) => {
    const queue = [clicked, ...list.filter((t) => t.id !== clicked.id)];
    onPlayTrack(clicked, queue);
  };

  return (
    <View style={[styles.container, style]}>
      {/* =================================================================== */}
      {/* SECTION 1: JUMP BACK IN (HORIZONTAL SCROLL)                         */}
      {/* =================================================================== */}
      {jumpBackInTracks && jumpBackInTracks.length > 0 && (
        <View style={styles.sectionWrap}>
          <SectionHeader
            title="Jump Back In"
            icon="🕒"
            actionText="Play All >"
            onActionPress={() => onPlayAll('Jump Back In', jumpBackInTracks)}
          />
          <FlatList
            horizontal
            data={jumpBackInTracks}
            keyExtractor={(item) => `jump_${item.id}`}
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.horizontalScrollContent}
            renderItem={({ item }) => (
              <SpringCard
                onPress={() => handlePlayFromList(item, jumpBackInTracks)}
                style={styles.horizontalCard}
              >
                <View style={styles.cardImageWrap}>
                  <Image source={{ uri: item.thumbnail }} style={styles.cardImage} resizeMode="cover" />
                  {/* Quick Play Button (32dp) */}
                  <View style={styles.quickPlayBtn}>
                    <View style={styles.quickPlayTriangle} />
                  </View>
                </View>
                <Text style={styles.cardTitle} numberOfLines={1}>
                  {item.title}
                </Text>
                <Text style={styles.cardArtist} numberOfLines={1}>
                  {item.artist}
                </Text>
              </SpringCard>
            )}
          />
        </View>
      )}

      {/* =================================================================== */}
      {/* SECTION 2: GLOBAL TOP CHARTS (HORIZONTAL WITH RANK BADGES)          */}
      {/* =================================================================== */}
      {topChartsTracks && topChartsTracks.length > 0 && (
        <View style={styles.sectionWrap}>
          <SectionHeader
            title="Global Top Charts"
            icon=""
            iconColor="#FC3C44" // Apple Red
            actionText="Play All >"
            onActionPress={() => onPlayAll('Global Top Charts', topChartsTracks)}
          />
          <FlatList
            horizontal
            data={topChartsTracks}
            keyExtractor={(item) => `charts_${item.id}`}
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.horizontalScrollContent}
            renderItem={({ item, index }) => (
              <SpringCard
                onPress={() => handlePlayFromList(item, topChartsTracks)}
                style={styles.horizontalCard}
              >
                <View style={styles.cardImageWrap}>
                  <Image source={{ uri: item.thumbnail }} style={styles.cardImage} resizeMode="cover" />
                  {/* Rank Badge (#1, #2...) */}
                  <View style={styles.rankBadge}>
                    <Text style={styles.rankBadgeText}>#{index + 1}</Text>
                  </View>
                  {/* Quick Play Button */}
                  <View style={styles.quickPlayBtn}>
                    <View style={styles.quickPlayTriangle} />
                  </View>
                </View>
                <Text style={styles.cardTitle} numberOfLines={1}>
                  {item.title}
                </Text>
                <Text style={styles.cardArtist} numberOfLines={1}>
                  {item.artist}
                </Text>
              </SpringCard>
            )}
          />
        </View>
      )}

      {/* =================================================================== */}
      {/* SECTION 3: MORE FROM {TOP ARTIST} (5 VERTICAL SONG ROWS)           */}
      {/* =================================================================== */}
      {topArtistName && topArtistTracks && topArtistTracks.length > 0 && (
        <View style={styles.sectionWrap}>
          <SectionHeader
            title={`✦ More from ${topArtistName}`}
            subtitle="Recommended songs based on the artist you love listening to"
            actionText="Play All >"
            onActionPress={() => onPlayAll(`More from ${topArtistName}`, topArtistTracks)}
          />
          <View style={styles.verticalRowsContainer}>
            {topArtistTracks.slice(0, 5).map((track, idx) => (
              <SongRow
                key={`artist_rec_${track.id}`}
                track={track}
                index={idx}
                isActive={playbackState.currentTrackId === track.id}
                isPlaying={playbackState.isPlaying}
                onPress={() => handlePlayFromList(track, topArtistTracks)}
                onToggleLike={() => onToggleLikeTrack?.(track)}
                onOptionsPress={() => onOpenTrackOptions?.(track)}
              />
            ))}
          </View>
        </View>
      )}

      {/* =================================================================== */}
      {/* SECTION 4: BASED ON YOUR TASTE (5 PERSONALIZED RECOMMENDATIONS)     */}
      {/* =================================================================== */}
      {tasteRecommendationTracks && tasteRecommendationTracks.length > 0 && (
        <View style={styles.sectionWrap}>
          <SectionHeader
            title="Based on Your Taste"
            actionText="Tune Taste ⚡"
            actionColor="#FF5A36"
            isPillAction
            onActionPress={onTuneTastePress}
          />
          <View style={styles.verticalRowsContainer}>
            {tasteRecommendationTracks.slice(0, 5).map((track, idx) => (
              <SongRow
                key={`taste_rec_${track.id}`}
                track={track}
                index={idx}
                isActive={playbackState.currentTrackId === track.id}
                isPlaying={playbackState.isPlaying}
                onPress={() => handlePlayFromList(track, tasteRecommendationTracks)}
                onToggleLike={() => onToggleLikeTrack?.(track)}
                onOptionsPress={() => onOpenTrackOptions?.(track)}
              />
            ))}
          </View>
        </View>
      )}

      {/* =================================================================== */}
      {/* SECTION 5: MADE FOR YOU (DAILY MIX WIDE FROSTED BANNER CARD)        */}
      {/* =================================================================== */}
      {dailyMixTracks && dailyMixTracks.length > 0 && (
        <View style={styles.sectionWrap}>
          <SectionHeader title="Made For You" />
          <SpringCard
            onPress={() => onPlayAll('Daily Mix', dailyMixTracks)}
            style={styles.dailyMixBannerCard}
          >
            {/* 135deg Soft Ambient Gradient Backdrop */}
            <LinearGradient
              colors={['rgba(255, 90, 54, 0.08)', 'rgba(20, 20, 20, 0.95)']}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
              style={StyleSheet.absoluteFillObject}
            />

            {/* Radiant Left Accent Stripe (3.5dp #FF5A36 with glow) */}
            <View style={styles.dailyMixLeftStripe} />

            {/* Content & Metadata */}
            <View style={styles.dailyMixContent}>
              <View style={styles.dailyMixTextCol}>
                <Text style={styles.dailyMixTitle} numberOfLines={1}>
                  Daily Mix • {topArtistName || 'Curated'} & More
                </Text>
                <Text style={styles.dailyMixSubtitle} numberOfLines={2}>
                  Personalized soundscape featuring {topArtistName || 'popular artists'} and similar vibes.
                </Text>
              </View>

              {/* Right Circular White Play Button (42x42dp) */}
              <View style={styles.dailyMixPlayBtn}>
                <View style={styles.dailyMixPlayTriangle} />
              </View>
            </View>
          </SpringCard>
        </View>
      )}

      {/* =================================================================== */}
      {/* SECTION 6: YOUR PLAYLISTS (HORIZONTAL CAROUSEL)                     */}
      {/* =================================================================== */}
      {userPlaylists && userPlaylists.length > 0 && (
        <View style={styles.sectionWrap}>
          <SectionHeader
            title="Your Playlists"
            actionText="See All >"
            onActionPress={onNavigateToLibrary}
          />
          <FlatList
            horizontal
            data={userPlaylists}
            keyExtractor={(item) => `user_playlist_${item.id}`}
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.horizontalScrollContent}
            renderItem={({ item }) => (
              <SpringCard
                onPress={() => onOpenPlaylist?.(item)}
                style={styles.horizontalCard}
              >
                <View style={styles.cardImageWrap}>
                  <Image
                    source={{
                      uri:
                        item.coverUrl ||
                        item.tracks[0]?.thumbnail ||
                        'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=400',
                    }}
                    style={styles.cardImage}
                    resizeMode="cover"
                  />
                </View>
                <Text style={styles.cardTitle} numberOfLines={1}>
                  {item.title}
                </Text>
                <Text style={styles.cardArtist} numberOfLines={1}>
                  {item.songCount || item.tracks.length} songs • By You
                </Text>
              </SpringCard>
            )}
          />
        </View>
      )}
    </View>
  );
};

// ============================================================================
// 4. STYLES & EXACT DESIGN TOKENS
// ============================================================================

const styles = StyleSheet.create({
  container: {
    width: '100%',
    paddingBottom: 40,
  },
  sectionWrap: {
    marginBottom: 26,
  },

  // --------------------------------------------------------------------------
  // Section Headers
  // --------------------------------------------------------------------------
  sectionHeaderContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    marginBottom: 12,
  },
  headerLeftCol: {
    flex: 1,
    paddingRight: 10,
  },
  headerTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  headerIcon: {
    fontSize: 16,
    marginRight: 4,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#FFFFFF',
    letterSpacing: -0.3,
  },
  headerSubtitle: {
    fontSize: 12,
    fontWeight: '400',
    color: '#A1A1AA',
    marginTop: 2,
  },
  actionTextBtn: {
    paddingVertical: 4,
  },
  actionText: {
    fontSize: 12.5,
    fontWeight: '600',
    color: '#A1A1AA',
  },
  actionPill: {
    paddingHorizontal: 12,
    paddingVertical: 5,
    borderRadius: 999,
    backgroundColor: 'rgba(255, 90, 54, 0.12)',
    borderWidth: 1,
    borderColor: 'rgba(255, 90, 54, 0.35)',
  },
  actionPillText: {
    fontSize: 11.5,
    fontWeight: '700',
    color: '#FF5A36',
  },

  // --------------------------------------------------------------------------
  // Horizontal Cards (Sections 1, 2, 6)
  // --------------------------------------------------------------------------
  horizontalScrollContent: {
    paddingHorizontal: 20,
    gap: 14,
  },
  horizontalCard: {
    width: 124,
    flexShrink: 0,
  },
  cardImageWrap: {
    width: 124,
    height: 124,
    borderRadius: 14,
    overflow: 'hidden',
    position: 'relative',
    backgroundColor: '#141414',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.10)',
    marginBottom: 8,
  },
  cardImage: {
    width: '100%',
    height: '100%',
  },
  quickPlayBtn: {
    position: 'absolute',
    bottom: 8,
    right: 8,
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
    ...Platform.select({
      ios: {
        shadowColor: '#000000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 4,
      },
      android: { elevation: 4 },
    }),
  },
  quickPlayTriangle: {
    width: 0,
    height: 0,
    borderLeftWidth: 8,
    borderRightWidth: 0,
    borderTopWidth: 5,
    borderBottomWidth: 5,
    borderStyle: 'solid',
    backgroundColor: 'transparent',
    borderLeftColor: '#090909',
    borderTopColor: 'transparent',
    borderBottomColor: 'transparent',
    marginLeft: 2,
  },
  rankBadge: {
    position: 'absolute',
    bottom: 8,
    left: 8,
    backgroundColor: 'rgba(0, 0, 0, 0.78)',
    borderWidth: 1,
    borderColor: 'rgba(255, 107, 74, 0.35)',
    borderRadius: 6,
    paddingHorizontal: 6,
    paddingVertical: 2,
  },
  rankBadgeText: {
    fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace',
    fontSize: 11,
    fontWeight: '800',
    color: '#FF6B4A',
  },
  cardTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: '#FFFFFF',
    lineHeight: 17,
  },
  cardArtist: {
    fontSize: 11,
    fontWeight: '500',
    color: '#A1A1AA',
    marginTop: 2,
  },

  // --------------------------------------------------------------------------
  // Vertical Song Rows (Sections 3 & 4)
  // --------------------------------------------------------------------------
  verticalRowsContainer: {
    paddingHorizontal: 20,
  },
  songRow: {
    height: 60,
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
  },
  songRowPressed: {
    opacity: 0.7,
  },
  songIndexCol: {
    width: 28,
    alignItems: 'center',
    justifyContent: 'center',
  },
  songIndexText: {
    fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace',
    fontSize: 11.5,
    fontWeight: '600',
    color: '#71717A',
  },
  songIndexActive: {
    color: '#FF5A36',
  },
  songThumbnail: {
    width: 44,
    height: 44,
    borderRadius: 10,
    backgroundColor: '#181818',
    marginLeft: 6,
    marginRight: 12,
  },
  songMetaCol: {
    flex: 1,
    justifyContent: 'center',
  },
  songTitle: {
    fontSize: 15,
    fontWeight: '600',
    color: '#FFFFFF',
  },
  songTitleActive: {
    fontWeight: '700',
    color: '#FF5A36',
  },
  songArtist: {
    fontSize: 13,
    fontWeight: '500',
    color: '#A1A1AA',
    marginTop: 2,
  },
  songDuration: {
    fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace',
    fontSize: 12,
    fontWeight: '500',
    color: '#71717A',
    marginRight: 12,
  },
  songActionBtn: {
    padding: 6,
    marginLeft: 2,
  },
  songLikeIcon: {
    fontSize: 16,
    color: '#71717A',
  },
  songLikeIconActive: {
    color: '#FF453A',
  },
  songOptionsIcon: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#71717A',
  },

  // Equalizer
  eqContainer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    height: 16,
    width: 14,
    justifyContent: 'space-between',
  },
  eqBar: {
    width: 3,
    backgroundColor: '#FF5A36',
    borderRadius: 1.5,
  },

  // --------------------------------------------------------------------------
  // Section 5: Made For You (Daily Mix)
  // --------------------------------------------------------------------------
  dailyMixBannerCard: {
    marginHorizontal: 20,
    height: 90,
    borderRadius: 18,
    overflow: 'hidden',
    position: 'relative',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.10)',
    backgroundColor: '#141414',
  },
  dailyMixLeftStripe: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    left: 0,
    width: 3.5,
    backgroundColor: '#FF5A36',
    ...Platform.select({
      ios: {
        shadowColor: '#FF5A36',
        shadowOffset: { width: 0, height: 0 },
        shadowOpacity: 0.9,
        shadowRadius: 8,
      },
      android: { elevation: 3 },
    }),
  },
  dailyMixContent: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingLeft: 18,
    paddingRight: 16,
  },
  dailyMixTextCol: {
    flex: 1,
    paddingRight: 12,
  },
  dailyMixTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: '#FFFFFF',
    marginBottom: 4,
  },
  dailyMixSubtitle: {
    fontSize: 11.5,
    fontWeight: '400',
    color: '#A1A1AA',
    lineHeight: 16,
  },
  dailyMixPlayBtn: {
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 5,
      },
      android: { elevation: 5 },
    }),
  },
  dailyMixPlayTriangle: {
    width: 0,
    height: 0,
    borderLeftWidth: 11,
    borderRightWidth: 0,
    borderTopWidth: 7,
    borderBottomWidth: 7,
    borderStyle: 'solid',
    backgroundColor: 'transparent',
    borderLeftColor: '#090909',
    borderTopColor: 'transparent',
    borderBottomColor: 'transparent',
    marginLeft: 3,
  },
});

export default HomeScreenSongSections;
