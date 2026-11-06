// Typed client for the SetMyDesk engine. Same API the website uses.
export const API_URL = (process.env.EXPO_PUBLIC_API_URL ?? 'https://setmydesk.com').replace(/\/$/, '');

export type IdeaStatus = 'ACCEPTED' | 'PENDING_TRANSCRIPTION' | 'DUPLICATE' | 'NEEDS_DETAIL';
export interface Outcome { status: IdeaStatus; ideaId: number | null; projectId: number | null; reply: string; trace: string[] }
export interface ProjectSummary { id: number; title: string; founder: string; totalPoints: number; holders: number; createdAt: string }
export interface Holder { contributorId: number; name: string; points: number; percent: number; contributions: number }
export interface IdeaView {
  id: number; contributor: string; channel: string; status: string; kind: 'FOUNDING' | 'ENHANCEMENT' | null; text: string;
  similarity: number | null; merit: number | null; points: number | null; projectId: number | null; projectTitle: string | null; createdAt: string;
}
export interface ProjectDetail { id: number; title: string; founder: string; createdAt: string; capTable: { totalPoints: number; holders: Holder[] }; ideas: IdeaView[] }

export type DeskItemType = 'MONITOR' | 'LAPTOP' | 'KEYBOARD' | 'MOUSE' | 'SPEAKER' | 'DESK_LAMP' | 'TABLET' | 'NOTEBOOK'
  | 'PHONE_STAND' | 'DOCUMENT_TRAY' | 'HEADPHONE_STAND' | 'PLANT' | 'MUG';
export interface CatalogItem { type: DeskItemType; label: string; widthCm: number; depthCm: number; heightCm: number; color: string }
export interface Placement {
  index: number; type: DeskItemType; label: string; x: number; z: number;
  widthCm: number; depthCm: number; heightCm: number; rotated: boolean; pinned: boolean; color: string;
}
export interface Layout {
  deskWidthCm: number; deskDepthCm: number; handedness: 'RIGHT' | 'LEFT'; placements: Placement[];
  unplaced: string[]; warnings: string[]; ergonomicScore: number; usedAreaPercent: number;
}
export interface LayoutRequest { deskWidthCm: number; deskDepthCm: number; handedness: 'RIGHT' | 'LEFT'; items: { type: DeskItemType }[] }

async function call<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(API_URL + path, init);
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error((body as { error?: string }).error ?? `Request failed (${res.status})`);
  return body as T;
}

const json = (body: unknown): RequestInit => ({ method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });

export const api = {
  submitIdea: (name: string, phone: string, text: string) => call<Outcome>('/api/ideas', json({ name, phone, text, channel: 'APP' })),
  submitVoice: (uri: string, name: string, phone: string, text: string) => {
    const form = new FormData();
    const ext = uri.split('.').pop()?.toLowerCase() || 'm4a';
    // React Native's FormData accepts { uri, name, type } file objects.
    form.append('audio', { uri, name: `idea.${ext}`, type: ext === 'webm' ? 'audio/webm' : 'audio/mp4' } as unknown as Blob);
    if (name) form.append('name', name);
    if (phone) form.append('phone', phone);
    if (text) form.append('text', text);
    return call<Outcome>('/api/ideas/voice', { method: 'POST', body: form });
  },
  projects: () => call<ProjectSummary[]>('/api/projects'),
  project: (id: number) => call<ProjectDetail>(`/api/projects/${id}`),
  catalog: () => call<CatalogItem[]>('/api/desk/catalog'),
  layout: (req: LayoutRequest) => call<Layout>('/api/desk/layout', json(req)),
};
