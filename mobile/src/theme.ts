import { useColorScheme } from 'react-native';

const light = { bg: '#fbf8f4', surface: '#ffffff', ink: '#1c1917', muted: '#6b625a', line: '#e7e0d8', brand: '#b4235a', brandInk: '#ffffff', ok: '#15803d', warn: '#b45309' };
const dark: typeof light = { bg: '#14110f', surface: '#1f1b18', ink: '#f5f0eb', muted: '#a8a097', line: '#342e29', brand: '#f472b6', brandInk: '#1c1917', ok: '#4ade80', warn: '#fbbf24' };

export type Theme = typeof light;
export const useTheme = (): Theme => (useColorScheme() === 'dark' ? dark : light);
