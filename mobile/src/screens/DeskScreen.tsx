import { useEffect, useState } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, View, useWindowDimensions } from 'react-native';
import { api, type CatalogItem, type DeskItemType, type Layout } from '../api';
import DeskAR from '../ar/DeskAR';
import { arStore } from '../ar/arStore';
import { useTheme } from '../theme';
import { Button, Card, Field, H1, Muted } from './ui';

const DEFAULTS: Partial<Record<DeskItemType, number>> = { MONITOR: 1, KEYBOARD: 1, MOUSE: 1, DESK_LAMP: 1, PLANT: 1, MUG: 1 };

export default function DeskScreen() {
  const t = useTheme();
  const { width: screenW } = useWindowDimensions();
  const [catalog, setCatalog] = useState<CatalogItem[]>([]);
  const [counts, setCounts] = useState<Partial<Record<DeskItemType, number>>>(DEFAULTS);
  const [w, setW] = useState('140');
  const [d, setD] = useState('70');
  const [hand, setHand] = useState<'RIGHT' | 'LEFT'>('RIGHT');
  const [layout, setLayout] = useState<Layout | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [arOpen, setArOpen] = useState(false);

  useEffect(() => { api.catalog().then(setCatalog).catch((e) => setError(e.message)); }, []);

  async function arrange() {
    const items = Object.entries(counts).flatMap(([type, n]) => Array.from({ length: n ?? 0 }, () => ({ type: type as DeskItemType })));
    if (!items.length) { setError('Pick at least one item.'); return; }
    setBusy(true); setError(null);
    try {
      setLayout(await api.layout({ deskWidthCm: Number(w), deskDepthCm: Number(d), handedness: hand, items }));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not arrange the desk');
    } finally {
      setBusy(false);
    }
  }

  const bump = (type: DeskItemType, delta: number) =>
    setCounts((c) => ({ ...c, [type]: Math.max(0, Math.min(4, (c[type] ?? 0) + delta)) }));

  // Top-down plan, scaled to the screen.
  const planW = Math.min(screenW - 64, 520);
  const scale = layout ? planW / layout.deskWidthCm : 1;

  return (
    <ScrollView contentContainerStyle={styles.pad}>
      <H1>Set my desk</H1>
      <Muted>Tell us your desk size and what's on it. The engine computes an exact, ergonomic layout, then you can see it on your real desk in AR.</Muted>
      <View style={{ height: 12 }} />
      <Card>
        <View style={styles.row}>
          <View style={{ flex: 1 }}><Field label="Width (cm)" value={w} onChangeText={setW} keyboardType="number-pad" maxLength={3} /></View>
          <View style={{ width: 12 }} />
          <View style={{ flex: 1 }}><Field label="Depth (cm)" value={d} onChangeText={setD} keyboardType="number-pad" maxLength={3} /></View>
        </View>
        <Text style={[styles.label, { color: t.ink }]}>Mouse hand</Text>
        <View style={[styles.row, { marginBottom: 12 }]}>
          {(['RIGHT', 'LEFT'] as const).map((h) => (
            <Pressable key={h} onPress={() => setHand(h)} accessibilityRole="radio" accessibilityState={{ checked: hand === h }}
                       style={[styles.chip, { borderColor: hand === h ? t.brand : t.line }]}>
              <Text style={{ color: hand === h ? t.brand : t.ink, fontWeight: '600' }}>{h === 'RIGHT' ? 'Right' : 'Left'}</Text>
            </Pressable>
          ))}
        </View>
        <Text style={[styles.label, { color: t.ink }]}>What's on your desk</Text>
        {catalog.map((c) => (
          <View key={c.type} style={[styles.itemRow, { borderColor: t.line }]}>
            <View style={[styles.swatch, { backgroundColor: c.color }]} />
            <Text style={{ color: t.ink, flex: 1 }}>{c.label}</Text>
            <Pressable onPress={() => bump(c.type, -1)} style={styles.step} accessibilityLabel={`Fewer ${c.label}`}><Text style={[styles.stepText, { color: t.ink }]}>−</Text></Pressable>
            <Text style={{ color: t.ink, width: 20, textAlign: 'center' }}>{counts[c.type] ?? 0}</Text>
            <Pressable onPress={() => bump(c.type, 1)} style={styles.step} accessibilityLabel={`More ${c.label}`}><Text style={[styles.stepText, { color: t.ink }]}>+</Text></Pressable>
          </View>
        ))}
        <View style={{ height: 12 }} />
        <Button title="Arrange my desk" onPress={arrange} busy={busy} />
      </Card>

      {error && <Card><Text style={{ color: t.warn }}>{error}</Text></Card>}

      {layout && (
        <Card>
          <Text style={[styles.score, { color: t.ink }]}>{layout.ergonomicScore}<Text style={{ fontSize: 16, fontWeight: '500' }}>/100 ergonomic score</Text></Text>
          <Muted>{layout.usedAreaPercent}% of the desk used · you sit at the bottom edge</Muted>
          <View style={[styles.plan, { width: planW, height: layout.deskDepthCm * scale, backgroundColor: '#d6b48a' }]}
                accessibilityLabel="Top-down desk plan">
            {layout.placements.map((p) => (
              <View key={p.index} accessibilityLabel={p.label} style={{
                position: 'absolute', backgroundColor: p.color, borderRadius: 3,
                left: (p.x - p.widthCm / 2 + layout.deskWidthCm / 2) * scale,
                top: (p.z - p.depthCm / 2 + layout.deskDepthCm / 2) * scale,
                width: p.widthCm * scale, height: p.depthCm * scale,
              }} />
            ))}
          </View>
          {layout.warnings.map((wn) => <Text key={wn} style={{ color: t.warn, marginBottom: 6 }}>• {wn}</Text>)}
          <Button title="📷 See it on my desk (AR)" onPress={() => { arStore.set(layout); setArOpen(true); }} />
        </Card>
      )}

      <Modal visible={arOpen} animationType="slide" onRequestClose={() => setArOpen(false)} presentationStyle="fullScreen">
        {arOpen && <DeskAR onClose={() => setArOpen(false)} />}
      </Modal>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  pad: { padding: 16, paddingBottom: 32 },
  row: { flexDirection: 'row', alignItems: 'center' },
  label: { fontSize: 14, fontWeight: '600', marginBottom: 6 },
  chip: { borderWidth: 1.5, borderRadius: 999, paddingHorizontal: 16, paddingVertical: 8, marginRight: 8 },
  itemRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 6, borderBottomWidth: StyleSheet.hairlineWidth },
  swatch: { width: 12, height: 12, borderRadius: 3, marginRight: 10 },
  step: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
  stepText: { fontSize: 22, fontWeight: '600' },
  score: { fontSize: 30, fontWeight: '800' },
  plan: { marginVertical: 14, borderRadius: 6, alignSelf: 'center', overflow: 'hidden' },
});
