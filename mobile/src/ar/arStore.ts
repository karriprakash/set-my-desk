// The AR scene is rendered by Viro's navigator, which takes a component, not props.
// The desk screen puts the layout here just before opening AR.
import type { Layout } from '../api';

let current: Layout | null = null;
export const arStore = {
  set: (layout: Layout) => { current = layout; },
  get: () => current,
};
