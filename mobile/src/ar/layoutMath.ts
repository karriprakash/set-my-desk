// Pure geometry shared by the AR scene: engine centimetres → AR metres. Unit tested.
import type { Layout, Placement } from '../api.ts';

/** Centimetres → metres. Division keeps values like 140 cm → 1.4 m exact. */
export const m = (cm: number) => cm / 100;

export interface ArBox { key: string; label: string; color: string; position: [number, number, number]; size: [number, number, number] }

/** Boxes positioned relative to the tapped point on the real desk surface (y = 0 is the surface). */
export function toArBoxes(layout: Layout): ArBox[] {
  return layout.placements.flatMap(boxesFor);
}

function boxesFor(p: Placement): ArBox[] {
  const w = m(p.widthCm), d = m(p.depthCm), h = Math.max(m(p.heightCm), 0.01);
  const x = m(p.x), z = m(p.z);
  if (p.type !== 'MONITOR') {
    return [{ key: `${p.index}`, label: p.label, color: p.color, position: [x, h / 2, z], size: [w, h, d] }];
  }
  // Monitor = base + neck + screen, so it reads as a monitor in AR.
  const screenH = 0.34, base = 0.02;
  const screenW = m(p.rotated ? p.depthCm : p.widthCm);
  return [
    { key: `${p.index}-base`, label: p.label, color: p.color, position: [x, base / 2, z], size: [w, base, d] },
    { key: `${p.index}-neck`, label: p.label, color: '#374151', position: [x, (h - screenH) / 2, z], size: [0.04, h - screenH, 0.03] },
    { key: `${p.index}-screen`, label: p.label, color: '#111827', position: [x, h - screenH / 2, z], size: [screenW, screenH, 0.02] },
  ];
}

/** Outline of the planned desk top, so you can check it matches your real desk. */
export function deskOutline(layout: Layout): { width: number; height: number } {
  return { width: m(layout.deskWidthCm), height: m(layout.deskDepthCm) };
}
