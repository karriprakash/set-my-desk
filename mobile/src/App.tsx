import { useState } from 'react';
import { Pressable, SafeAreaView, StyleSheet, Text, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { useTheme } from './theme';
import IdeaScreen from './screens/IdeaScreen';
import ProjectsScreen from './screens/ProjectsScreen';
import DeskScreen from './screens/DeskScreen';

type Tab = 'idea' | 'projects' | 'desk';
const TABS: { key: Tab; label: string; icon: string }[] = [
  { key: 'idea', label: 'Share idea', icon: '💡' },
  { key: 'projects', label: 'Projects', icon: '📊' },
  { key: 'desk', label: 'Set my desk', icon: '🖥' },
];

export default function App() {
  const t = useTheme();
  const [tab, setTab] = useState<Tab>('idea');
  const [openProject, setOpenProject] = useState<number | null>(null);

  return (
    <SafeAreaView style={[styles.root, { backgroundColor: t.bg }]}>
      <StatusBar style="auto" />
      <View style={[styles.header, { borderColor: t.line, backgroundColor: t.surface }]}>
        <Text style={[styles.brand, { color: t.ink }]}>Set<Text style={{ color: t.brand }}>My</Text>Desk</Text>
      </View>
      <View style={styles.body}>
        {tab === 'idea' && <IdeaScreen onOpenProject={(id) => { setOpenProject(id); setTab('projects'); }} />}
        {tab === 'projects' && <ProjectsScreen openId={openProject} onOpen={setOpenProject} />}
        {tab === 'desk' && <DeskScreen />}
      </View>
      <View style={[styles.tabs, { borderColor: t.line, backgroundColor: t.surface }]} accessibilityRole="tablist">
        {TABS.map((item) => {
          const active = tab === item.key;
          return (
            <Pressable key={item.key} style={styles.tab} onPress={() => setTab(item.key)}
                       accessibilityRole="tab" accessibilityState={{ selected: active }}>
              <Text style={styles.icon}>{item.icon}</Text>
              <Text style={[styles.tabLabel, { color: active ? t.brand : t.muted, fontWeight: active ? '700' : '500' }]}>{item.label}</Text>
            </Pressable>
          );
        })}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  header: { paddingHorizontal: 16, paddingVertical: 12, borderBottomWidth: StyleSheet.hairlineWidth },
  brand: { fontSize: 20, fontWeight: '800', letterSpacing: -0.4 },
  body: { flex: 1 },
  tabs: { flexDirection: 'row', borderTopWidth: StyleSheet.hairlineWidth, paddingBottom: 4 },
  tab: { flex: 1, alignItems: 'center', paddingVertical: 8, minHeight: 56 },
  icon: { fontSize: 20 },
  tabLabel: { fontSize: 12, marginTop: 2 },
});
