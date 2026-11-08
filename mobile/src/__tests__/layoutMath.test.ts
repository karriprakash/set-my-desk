import { test } from 'node:test';
import assert from 'node:assert/strict';
import { toArBoxes, deskOutline } from '../ar/layoutMath.ts';
import type { Layout } from '../api.ts';

const layout: Layout = {
  deskWidthCm: 140, deskDepthCm: 70, handedness: 'RIGHT', unplaced: [], warnings: [], ergonomicScore: 100, usedAreaPercent: 20,
  placements: [
    { index: 0, type: 'MONITOR', label: 'Monitor', x: 0, z: -25, widthCm: 61, depthCm: 20, heightCm: 45, rotated: false, pinned: false, color: '#1f2937' },
    { index: 1, type: 'MUG', label: 'Mug', x: 50, z: 10, widthCm: 9, depthCm: 9, heightCm: 10, rotated: false, pinned: false, color: '#dc2626' },
  ],
};

test('converts centimetres to metres and rests items on the surface', () => {
  const mug = toArBoxes(layout).find((b) => b.key === '1')!;
  assert.deepEqual(mug.position, [0.5, 0.05, 0.1]);
  assert.deepEqual(mug.size, [0.09, 0.1, 0.09]);
});

test('monitor becomes base, neck and screen that top out at its real height', () => {
  const parts = toArBoxes(layout).filter((b) => b.key.startsWith('0-'));
  assert.equal(parts.length, 3);
  const screen = parts.find((b) => b.key === '0-screen')!;
  assert.ok(Math.abs(screen.position[1] + screen.size[1] / 2 - 0.45) < 1e-9);
});

test('desk outline matches the desk size', () => {
  assert.deepEqual(deskOutline(layout), { width: 1.4, height: 0.7 });
});
