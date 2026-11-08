// Native AR (ARCore on Android, ARKit on iOS) via ViroReact. Needs a development build:
// `npx expo prebuild && npx expo run:android` (Expo Go does not include Viro).
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import {
  ViroAmbientLight, ViroARPlaneSelector, ViroARScene, ViroARSceneNavigator, ViroBox, ViroDirectionalLight,
  ViroMaterials, ViroNode, ViroQuad, ViroText, ViroTrackingStateConstants,
} from '@reactvision/react-viro';
import type { Layout } from '../api';
import { arStore } from './arStore';
import { deskOutline, toArBoxes } from './layoutMath';

const materialFor = (hex: string) => `c_${hex.replace('#', '')}`;

function registerMaterials(layout: Layout) {
  const dict: Record<string, { diffuseColor: string; lightingModel: 'Lambert' }> = {
    c_outline: { diffuseColor: 'rgba(180,35,90,0.25)', lightingModel: 'Lambert' },
  };
  for (const b of toArBoxes(layout)) dict[materialFor(b.color)] = { diffuseColor: b.color, lightingModel: 'Lambert' };
  ViroMaterials.createMaterials(dict);
}

function DeskScene() {
  const layout = arStore.get();
  const [hint, setHint] = useState('Move your phone slowly over your desk…');
  if (!layout) return <ViroARScene />;
  registerMaterials(layout);
  const outline = deskOutline(layout);

  return (
    <ViroARScene onTrackingUpdated={(state) => {
      if (state === ViroTrackingStateConstants.TRACKING_NORMAL) setHint('Tap the highlighted surface to place your desk layout');
    }}>
      <ViroAmbientLight color="#ffffff" intensity={300} />
      <ViroDirectionalLight color="#ffffff" direction={[0, -1, -0.5]} />
      <ViroText text={hint} position={[0, 0.1, -1]} scale={[0.15, 0.15, 0.15]} style={styles.arHint} />
      <ViroARPlaneSelector alignment="Horizontal" minWidth={0.3} minHeight={0.2} onPlaneSelected={() => setHint('')}>
        <ViroNode>
          <ViroQuad width={outline.width} height={outline.height} rotation={[-90, 0, 0]} materials={['c_outline']} />
          {toArBoxes(layout).map((b) => (
            <ViroBox key={b.key} position={b.position} width={b.size[0]} height={b.size[1]} length={b.size[2]} materials={[materialFor(b.color)]} />
          ))}
        </ViroNode>
      </ViroARPlaneSelector>
    </ViroARScene>
  );
}

export default function DeskAR({ onClose }: { onClose: () => void }) {
  return (
    <View style={StyleSheet.absoluteFill}>
      <ViroARSceneNavigator autofocus initialScene={{ scene: DeskScene }} style={StyleSheet.absoluteFill} />
      <Pressable onPress={onClose} style={styles.close} accessibilityRole="button" accessibilityLabel="Close AR">
        <Text style={styles.closeText}>✕ Close</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  arHint: { fontSize: 20, color: '#ffffff', textAlign: 'center' },
  close: { position: 'absolute', top: 48, right: 16, backgroundColor: 'rgba(0,0,0,0.6)', borderRadius: 999, paddingHorizontal: 16, paddingVertical: 10 },
  closeText: { color: '#fff', fontWeight: '700', fontSize: 16 },
});
