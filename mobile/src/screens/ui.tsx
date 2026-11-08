import type { ReactNode } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, TextInput, View, type TextInputProps } from 'react-native';
import { useTheme } from '../theme';

export function Card({ children }: { children: ReactNode }) {
  const t = useTheme();
  return <View style={[s.card, { backgroundColor: t.surface, borderColor: t.line }]}>{children}</View>;
}

export function H1({ children }: { children: ReactNode }) {
  return <Text style={[s.h1, { color: useTheme().ink }]} accessibilityRole="header">{children}</Text>;
}

export function Muted({ children }: { children: ReactNode }) {
  return <Text style={[s.muted, { color: useTheme().muted }]}>{children}</Text>;
}

export function Field({ label, ...props }: TextInputProps & { label: string }) {
  const t = useTheme();
  return (
    <View style={s.field}>
      <Text style={[s.label, { color: t.ink }]}>{label}</Text>
      <TextInput placeholderTextColor={t.muted} accessibilityLabel={label} {...props}
                 style={[s.input, { color: t.ink, borderColor: t.line, backgroundColor: t.bg }, props.multiline && s.multiline]} />
    </View>
  );
}

export function Button({ title, onPress, kind = 'primary', busy, disabled }:
  { title: string; onPress: () => void; kind?: 'primary' | 'secondary'; busy?: boolean; disabled?: boolean }) {
  const t = useTheme();
  const primary = kind === 'primary';
  return (
    <Pressable onPress={onPress} disabled={busy || disabled} accessibilityRole="button"
               style={[s.button, primary ? { backgroundColor: t.brand } : { borderColor: t.line, borderWidth: 1 }, (busy || disabled) && { opacity: 0.5 }]}>
      {busy ? <ActivityIndicator color={primary ? t.brandInk : t.ink} />
        : <Text style={[s.buttonText, { color: primary ? t.brandInk : t.ink }]}>{title}</Text>}
    </Pressable>
  );
}

const s = StyleSheet.create({
  card: { borderWidth: 1, borderRadius: 14, padding: 16, marginBottom: 12 },
  h1: { fontSize: 28, fontWeight: '800', letterSpacing: -0.5, marginBottom: 4 },
  muted: { fontSize: 14, lineHeight: 20 },
  field: { marginBottom: 12 },
  label: { fontSize: 14, fontWeight: '600', marginBottom: 6 },
  input: { borderWidth: 1, borderRadius: 10, paddingHorizontal: 12, paddingVertical: 10, fontSize: 16 },
  multiline: { minHeight: 120, textAlignVertical: 'top' },
  button: { borderRadius: 999, paddingVertical: 12, paddingHorizontal: 18, alignItems: 'center', minHeight: 48, justifyContent: 'center' },
  buttonText: { fontSize: 16, fontWeight: '700' },
});
