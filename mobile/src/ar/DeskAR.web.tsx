// Web build of the app: point to the website, which has WebXR AR.
import { Linking, StyleSheet, Text, View } from 'react-native';
import { API_URL } from '../api';
import { Button } from '../screens/ui';

export default function DeskAR({ onClose }: { onClose: () => void }) {
  return (
    <View style={styles.wrap}>
      <Text style={styles.text}>AR in the browser runs on the SetMyDesk website (Chrome on Android). On iPhone and Android, install the app for full AR.</Text>
      <Button title="Open AR on the website" onPress={() => Linking.openURL(`${API_URL}/#desk`)} />
      <View style={{ height: 10 }} />
      <Button kind="secondary" title="Back" onPress={onClose} />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { position: 'absolute', top: 0, right: 0, bottom: 0, left: 0, padding: 24, justifyContent: 'center', backgroundColor: '#14110f' },
  text: { color: '#f5f0eb', fontSize: 16, lineHeight: 23, marginBottom: 20, textAlign: 'center' },
});
