import { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { RecordingPresets, requestRecordingPermissionsAsync, setAudioModeAsync, useAudioRecorder, useAudioRecorderState } from 'expo-audio';
import { api, type Outcome } from '../api';
import { useTheme } from '../theme';
import { Button, Card, Field, H1, Muted } from './ui';

export default function IdeaScreen({ onOpenProject }: { onOpenProject: (id: number) => void }) {
  const t = useTheme();
  const recorder = useAudioRecorder(RecordingPresets.HIGH_QUALITY);
  const recState = useAudioRecorderState(recorder);
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [text, setText] = useState('');
  const [voiceUri, setVoiceUri] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<Outcome | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function toggleRecording() {
    setError(null);
    if (recState.isRecording) {
      await recorder.stop();
      setVoiceUri(recorder.uri);
      return;
    }
    const perm = await requestRecordingPermissionsAsync();
    if (!perm.granted) { setError('Microphone permission is needed for voice notes. You can type your idea instead.'); return; }
    await setAudioModeAsync({ allowsRecording: true, playsInSilentMode: true });
    await recorder.prepareToRecordAsync();
    recorder.record();
  }

  async function send() {
    if (!text.trim() && !voiceUri) { setError('Please type your idea or record a voice note.'); return; }
    setBusy(true); setError(null);
    try {
      const out = voiceUri ? await api.submitVoice(voiceUri, name, phone, text.trim()) : await api.submitIdea(name, phone, text.trim());
      setResult(out);
      if (out.status === 'ACCEPTED' || out.status === 'PENDING_TRANSCRIPTION') { setText(''); setVoiceUri(null); }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Something went wrong');
    } finally {
      setBusy(false);
    }
  }

  const seconds = Math.round((recState.durationMillis ?? 0) / 1000);
  const good = result && (result.status === 'ACCEPTED' || result.status === 'PENDING_TRANSCRIPTION');

  return (
    <ScrollView contentContainerStyle={styles.pad} keyboardShouldPersistTaps="handled">
      <H1>Your idea. Your shares.</H1>
      <Muted>Free forever, no money in or out. Speak or type your idea. If it already exists, it will improve that project and you will still earn your fair share.</Muted>
      <View style={{ height: 16 }} />
      <Card>
        <Field label="Your name" value={name} onChangeText={setName} placeholder="e.g. Lakshmi" autoComplete="name" maxLength={80} />
        <Field label="Phone (optional, kept private)" value={phone} onChangeText={setPhone} placeholder="+91…" keyboardType="phone-pad" maxLength={20} />
        <Field label="Your idea" value={text} onChangeText={setText} multiline maxLength={10000}
               placeholder="Who has the problem? What is it? How would your idea solve it?" />
        <Button kind="secondary" onPress={toggleRecording}
                title={recState.isRecording ? `⏹ Stop (${seconds}s)` : voiceUri ? '🎙 Record again' : '🎙 Record voice note'} />
        {voiceUri && !recState.isRecording && <Text style={[styles.note, { color: t.ok }]}>Voice note ready ✓</Text>}
        <View style={{ height: 10 }} />
        <Button title="Send my idea" onPress={send} busy={busy} disabled={recState.isRecording} />
      </Card>
      {error && <Card><Text style={{ color: t.warn }}>{error}</Text></Card>}
      {result && (
        <Card>
          <Text style={[styles.reply, { color: good ? t.ink : t.warn }]}>{result.reply}</Text>
          {result.projectId != null && <Button kind="secondary" title="See the project →" onPress={() => onOpenProject(result.projectId!)} />}
        </Card>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  pad: { padding: 16, paddingBottom: 32 },
  note: { marginTop: 8, fontWeight: '600' },
  reply: { fontSize: 16, lineHeight: 23, marginBottom: 12 },
});
