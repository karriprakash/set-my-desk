// SetMyDesk web app: ideas → shares, and the AR desk planner. No build step.
const API = window.SETMYDESK_API || '';
const $ = (sel, root = document) => root.querySelector(sel);
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

async function api(path, opts = {}) {
  const res = await fetch(API + path, opts);
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(body.error || `Request failed (${res.status})`);
  return body;
}

// ---------- router ----------
const views = ['idea', 'projects', 'desk', 'rules'];
const loaders = { projects: loadProjects, desk: loadDesk, rules: loadRules };
function route() {
  const [view, arg] = (location.hash.slice(1) || 'idea').split('/');
  const current = views.includes(view) ? view : 'idea';
  views.forEach((v) => { $('#' + v).hidden = v !== current; });
  document.querySelectorAll('nav a').forEach((a) => a.classList.toggle('active', a.getAttribute('href') === '#' + current));
  loaders[current]?.(arg);
}
addEventListener('hashchange', route);

// ---------- share an idea ----------
let recorder = null, chunks = [], voiceBlob = null;
$('#record').addEventListener('click', async () => {
  const btn = $('#record'), status = $('#rec-status');
  if (recorder?.state === 'recording') { recorder.stop(); return; }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    recorder = new MediaRecorder(stream);
    chunks = [];
    recorder.ondataavailable = (e) => chunks.push(e.data);
    recorder.onstop = () => {
      stream.getTracks().forEach((t) => t.stop());
      voiceBlob = new Blob(chunks, { type: recorder.mimeType || 'audio/webm' });
      btn.textContent = '🎙 Record again';
      btn.setAttribute('aria-pressed', 'false');
      status.textContent = `Voice note ready (${Math.round(voiceBlob.size / 1024)} KB)`;
    };
    recorder.start();
    btn.textContent = '⏹ Stop recording';
    btn.setAttribute('aria-pressed', 'true');
    status.textContent = 'Recording… speak your idea';
  } catch {
    status.textContent = 'Microphone not available. You can type your idea instead.';
  }
});

$('#idea-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.target, data = new FormData(form), out = $('#idea-result');
  const submit = form.querySelector('[type=submit]');
  const text = data.get('text').trim();
  if (!text && !voiceBlob) { show(out, 'Please type your idea or record a voice note.', true); return; }
  submit.disabled = true;
  try {
    let result;
    if (voiceBlob) {
      const fd = new FormData();
      fd.append('audio', voiceBlob, 'idea.' + (voiceBlob.type.includes('mp4') ? 'm4a' : 'webm'));
      ['name', 'phone'].forEach((k) => data.get(k) && fd.append(k, data.get(k)));
      if (text) fd.append('text', text);
      result = await api('/api/ideas/voice', { method: 'POST', body: fd });
    } else {
      result = await api('/api/ideas', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: data.get('name'), phone: data.get('phone'), text, channel: 'WEB' }),
      });
    }
    const link = result.projectId ? ` <a href="#projects/${result.projectId}">See the project →</a>` : '';
    show(out, esc(result.reply) + link, result.status !== 'ACCEPTED' && result.status !== 'PENDING_TRANSCRIPTION', true);
    if (result.status === 'ACCEPTED' || result.status === 'PENDING_TRANSCRIPTION') {
      form.text.value = ''; voiceBlob = null; $('#rec-status').textContent = ''; $('#record').textContent = '🎙 Record voice note';
    }
  } catch (err) {
    show(out, err.message, true);
  } finally {
    submit.disabled = false;
  }
});

function show(el, msg, warn = false, html = false) {
  el.hidden = false;
  el.classList.toggle('warn', warn);
  if (html) el.innerHTML = msg; else el.textContent = msg;
}

// ---------- projects ----------
async function loadProjects(id) {
  const list = $('#project-list'), detail = $('#project-detail');
  try {
    const projects = await api('/api/projects');
    list.innerHTML = projects.length ? projects.map((p) => `
      <a class="project" href="#projects/${p.id}">
        <h3>${esc(p.title)}</h3>
        <div class="muted">Founded by ${esc(p.founder)} · ${p.holders} contributor${p.holders === 1 ? '' : 's'} · ${Math.round(p.totalPoints)} points</div>
      </a>`).join('') : '<p class="muted">No projects yet. <a href="#idea">Be the first to share an idea.</a></p>';
  } catch (err) { list.textContent = err.message; }

  if (!id) { detail.hidden = true; return; }
  try {
    const p = await api('/api/projects/' + encodeURIComponent(id));
    detail.hidden = false;
    detail.innerHTML = `
      <h2>${esc(p.title)}</h2>
      <p class="muted">Founded by ${esc(p.founder)} on ${new Date(p.createdAt).toLocaleDateString()}</p>
      <h3>Shareholders</h3>
      <table><thead><tr><th>Name</th><th class="num">Points</th><th class="num">Share</th><th></th></tr></thead><tbody>
      ${p.capTable.holders.map((h) => `<tr><td>${esc(h.name)}</td><td class="num">${h.points.toFixed(0)}</td>
        <td class="num">${h.percent.toFixed(1)}%</td><td><div class="bar"><i style="width:${h.percent}%"></i></div></td></tr>`).join('')}
      </tbody></table>
      <h3>Ideas in this project</h3>
      ${p.ideas.map((i) => `<div class="idea-item">
        <span class="tag">${i.kind === 'FOUNDING' ? 'Founding idea' : 'Enhancement'}</span>
        <span class="tag">${esc(i.channel.replace('_', ' ').toLowerCase())}</span>
        <b>${esc(i.contributor)}</b> · ${i.points.toFixed(0)} pts (merit ${i.merit.toFixed(2)}${i.kind === 'ENHANCEMENT' ? `, overlap ${(i.similarity * 100).toFixed(0)}%` : ''})
        <p>${esc(i.text)}</p></div>`).join('')}`;
    detail.scrollIntoView({ behavior: 'smooth', block: 'start' });
  } catch (err) { detail.hidden = false; detail.textContent = err.message; }
}

// ---------- rules ----------
async function loadRules() {
  const r = await api('/api/rules');
  $('#rules-body').innerHTML = `
    <p><b>${esc(r.cost)}</b></p>
    <p><code>${esc(r.formula)}</code> with basePoints = ${r.basePoints}</p>
    <ul>
      <li><b>Merit</b> = <code>${esc(r.merit)}</code>. It scores the idea's substance, not who you are.</li>
      <li><b>Novelty</b>: ${esc(r.novelty)}. An idea counts as improving an existing project when overlap ≥ ${(r.enhancementThreshold * 100).toFixed(0)}%.</li>
      <li><b>Fair over time</b>: ${esc(r.timeInvariance)}</li>
      <li><b>Your share %</b> = ${esc(r.sharePercent)}</li>
      <li><b>Ledger</b>: ${esc(r.ledger)}</li>
    </ul>
    <h3>The engine's 12 cylinders</h3>
    <ol>${r.firingOrder.map((c) => `<li>${esc(c.replace(/^\d+-/, '').replace('-', ' '))}</li>`).join('')}</ol>`;
}

// ---------- desk ----------
let deskScene = null, catalogLoaded = false;
const DEFAULT_ITEMS = { MONITOR: 1, KEYBOARD: 1, MOUSE: 1, DESK_LAMP: 1, PLANT: 1, MUG: 1 };

async function loadDesk() {
  if (!catalogLoaded) {
    const catalog = await api('/api/desk/catalog');
    $('#catalog').insertAdjacentHTML('beforeend', catalog.map((c) => `
      <div class="cat-row"><span><i class="swatch" style="background:${c.color}"></i>${esc(c.label)}
        <span class="muted">${c.widthCm}×${c.depthCm} cm</span></span>
        <input type="number" min="0" max="4" value="${DEFAULT_ITEMS[c.type] || 0}" name="item-${c.type}" aria-label="${esc(c.label)} count"></div>`).join(''));
    catalogLoaded = true;
  }
  if (!deskScene) {
    const { DeskScene } = await import('./desk3d.js');
    deskScene = new DeskScene($('#stage'), $('#ar-slot'));
    arrange();
  }
}

$('#desk-form').addEventListener('submit', (e) => { e.preventDefault(); arrange(); });

async function arrange() {
  const f = $('#desk-form'), report = $('#desk-report');
  const items = [];
  f.querySelectorAll('[name^=item-]').forEach((input) => {
    for (let i = 0; i < Number(input.value || 0); i++) items.push({ type: input.name.slice(5) });
  });
  if (!items.length) { report.hidden = false; report.textContent = 'Pick at least one item.'; return; }
  try {
    const layout = await api('/api/desk/layout', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deskWidthCm: Number(f.w.value), deskDepthCm: Number(f.d.value), handedness: f.hand.value, items }),
    });
    deskScene?.render(layout);
    report.hidden = false;
    report.innerHTML = `<div><span class="score">${layout.ergonomicScore}</span>/100 ergonomic score ·
      <span class="muted">${layout.usedAreaPercent}% of the desk used</span></div>
      ${layout.warnings.length ? `<ul class="warnings">${layout.warnings.map((w) => `<li>${esc(w)}</li>`).join('')}</ul>` : '<p class="muted">Looks great. No issues found.</p>'}`;
  } catch (err) { report.hidden = false; report.textContent = err.message; }
}

route();
