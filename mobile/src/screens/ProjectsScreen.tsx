import { useCallback, useEffect, useState } from 'react';
import { Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { api, type ProjectDetail, type ProjectSummary } from '../api';
import { useTheme } from '../theme';
import { Button, Card, H1, Muted } from './ui';

export default function ProjectsScreen({ openId, onOpen }: { openId: number | null; onOpen: (id: number | null) => void }) {
  const t = useTheme();
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [detail, setDetail] = useState<ProjectDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try {
      setProjects(await api.projects());
      setDetail(openId != null ? await api.project(openId) : null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not load projects');
    } finally {
      setLoading(false);
    }
  }, [openId]);

  useEffect(() => { load(); }, [load]);

  if (detail) {
    return (
      <ScrollView contentContainerStyle={styles.pad}>
        <Button kind="secondary" title="← All projects" onPress={() => onOpen(null)} />
        <View style={{ height: 12 }} />
        <H1>{detail.title}</H1>
        <Muted>Founded by {detail.founder}</Muted>
        <View style={{ height: 12 }} />
        <Card>
          <Text style={[styles.h2, { color: t.ink }]}>Shareholders</Text>
          {detail.capTable.holders.map((h) => (
            <View key={h.contributorId} style={styles.holder}>
              <View style={styles.holderRow}>
                <Text style={[styles.name, { color: t.ink }]}>{h.name}</Text>
                <Text style={{ color: t.ink, fontVariant: ['tabular-nums'] }}>{h.points.toFixed(0)} pts · {h.percent.toFixed(1)}%</Text>
              </View>
              <View style={[styles.bar, { backgroundColor: t.line }]}>
                <View style={{ width: `${h.percent}%`, height: '100%', backgroundColor: t.brand }} />
              </View>
            </View>
          ))}
        </Card>
        <Text style={[styles.h2, { color: t.ink }]}>Ideas in this project</Text>
        {detail.ideas.map((i) => (
          <Card key={i.id}>
            <Text style={[styles.tag, { color: t.muted }]}>
              {i.kind === 'FOUNDING' ? 'Founding idea' : 'Enhancement'} · {i.channel.replace('_', ' ').toLowerCase()}
            </Text>
            <Text style={[styles.name, { color: t.ink }]}>{i.contributor} · {(i.points ?? 0).toFixed(0)} pts</Text>
            <Text style={{ color: t.ink, marginTop: 6, lineHeight: 21 }}>{i.text}</Text>
          </Card>
        ))}
      </ScrollView>
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.pad} refreshControl={<RefreshControl refreshing={loading} onRefresh={load} />}>
      <H1>Projects</H1>
      <Muted>Every project started from someone's idea. Pull down to refresh.</Muted>
      <View style={{ height: 12 }} />
      {error && <Card><Text style={{ color: t.warn }}>{error}</Text></Card>}
      {!loading && !error && projects.length === 0 && <Muted>No projects yet. Be the first to share an idea!</Muted>}
      {projects.map((p) => (
        <Pressable key={p.id} onPress={() => onOpen(p.id)} accessibilityRole="button">
          <Card>
            <Text style={[styles.name, { color: t.ink, fontSize: 17 }]}>{p.title}</Text>
            <Muted>Founded by {p.founder} · {p.holders} contributor{p.holders === 1 ? '' : 's'} · {Math.round(p.totalPoints)} points</Muted>
          </Card>
        </Pressable>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  pad: { padding: 16, paddingBottom: 32 },
  h2: { fontSize: 18, fontWeight: '700', marginBottom: 10 },
  holder: { marginBottom: 12 },
  holderRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 4 },
  name: { fontWeight: '700', fontSize: 15 },
  bar: { height: 6, borderRadius: 3, overflow: 'hidden' },
  tag: { fontSize: 12, marginBottom: 4 },
});
