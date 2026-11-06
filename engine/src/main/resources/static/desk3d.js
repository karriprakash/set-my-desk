// 3D preview + WebXR AR placement of a desk layout computed by the engine.
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { ARButton } from 'three/addons/webxr/ARButton.js';

const CM = 0.01; // engine works in centimetres, three.js in metres
const DESK_HEIGHT = 0.75;

export class DeskScene {
  constructor(container, arSlot) {
    this.container = container;
    this.scene = new THREE.Scene();
    this.camera = new THREE.PerspectiveCamera(45, 4 / 3, 0.01, 50);
    this.camera.position.set(0, 1.6, 1.5);

    this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    this.renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
    this.renderer.xr.enabled = true;
    container.appendChild(this.renderer.domElement);

    this.controls = new OrbitControls(this.camera, this.renderer.domElement);
    this.controls.target.set(0, DESK_HEIGHT, 0);
    this.controls.enableDamping = true;

    this.scene.add(new THREE.HemisphereLight(0xffffff, 0x8d7f74, 2.2));
    const sun = new THREE.DirectionalLight(0xffffff, 1.6);
    sun.position.set(1, 3, 2);
    this.scene.add(sun);

    this.preview = new THREE.Group();   // desk with legs for the on-screen preview
    this.arGroup = new THREE.Group();   // items only, dropped onto your real desk in AR
    this.arGroup.visible = false;
    this.scene.add(this.preview, this.arGroup);

    this.#setupAR(arSlot);
    new ResizeObserver(() => this.#resize()).observe(container);
    this.#resize();
    this.renderer.setAnimationLoop((t, frame) => this.#tick(frame));
  }

  render(layout) {
    this.layout = layout;
    this.preview.clear();
    this.arGroup.clear();

    const W = layout.deskWidthCm * CM, D = layout.deskDepthCm * CM;
    const top = box(W, 0.03, D, 0xd6b48a);
    top.position.y = DESK_HEIGHT - 0.015;
    this.preview.add(top);
    for (const sx of [-1, 1]) for (const sz of [-1, 1]) {
      const leg = box(0.04, DESK_HEIGHT - 0.03, 0.04, 0x57534e);
      leg.position.set(sx * (W / 2 - 0.05), (DESK_HEIGHT - 0.03) / 2, sz * (D / 2 - 0.05));
      this.preview.add(leg);
    }
    const items = buildItems(layout);
    items.position.y = DESK_HEIGHT;
    this.preview.add(items);

    // AR: a faint outline of the planned desk area plus the items, sitting on the detected surface.
    const outline = new THREE.Mesh(new THREE.PlaneGeometry(W, D), new THREE.MeshBasicMaterial({ color: 0xb4235a, transparent: true, opacity: 0.18 }));
    outline.rotation.x = -Math.PI / 2;
    this.arGroup.add(outline, buildItems(layout));

    this.controls.target.set(0, DESK_HEIGHT, 0);
    this.camera.position.set(0, DESK_HEIGHT + Math.max(W, D) * 0.9, D / 2 + Math.max(W, D) * 0.9);
  }

  #setupAR(slot) {
    if (!navigator.xr) { slot.innerHTML = '<p class="muted">AR needs a phone browser with WebXR (Chrome on Android). The 3D preview works everywhere.</p>'; return; }
    navigator.xr.isSessionSupported('immersive-ar').then((ok) => {
      if (!ok) { slot.innerHTML = '<p class="muted">This device does not support AR in the browser. Use the SetMyDesk app, or open this page in Chrome on Android.</p>'; return; }
      const overlay = document.createElement('div');
      overlay.style.cssText = 'position:fixed;inset:auto 0 24px;text-align:center;color:#fff;font:600 16px system-ui;text-shadow:0 1px 3px #000';
      overlay.textContent = 'Point at your desk, then tap to place the layout';
      document.body.appendChild(overlay);
      overlay.hidden = true;
      const button = ARButton.createButton(this.renderer, { requiredFeatures: ['hit-test'], optionalFeatures: ['dom-overlay'], domOverlay: { root: overlay } });
      slot.appendChild(button);

      this.reticle = new THREE.Mesh(new THREE.RingGeometry(0.06, 0.08, 32).rotateX(-Math.PI / 2), new THREE.MeshBasicMaterial({ color: 0xffffff }));
      this.reticle.matrixAutoUpdate = false;
      this.reticle.visible = false;
      this.scene.add(this.reticle);

      const controller = this.renderer.xr.getController(0);
      controller.addEventListener('select', () => {
        if (!this.reticle.visible) return;
        this.reticle.matrix.decompose(this.arGroup.position, this.arGroup.quaternion, new THREE.Vector3());
        // face the layout towards the person holding the phone
        const cam = this.renderer.xr.getCamera();
        const dir = new THREE.Vector3().subVectors(cam.position, this.arGroup.position);
        this.arGroup.rotation.set(0, Math.atan2(dir.x, dir.z), 0);
        this.arGroup.visible = true;
        overlay.textContent = 'Tap again to move it';
      });
      this.scene.add(controller);

      this.renderer.xr.addEventListener('sessionstart', () => { this.preview.visible = false; overlay.hidden = false; this.hitSource = null; this.#requestHitSource(); });
      this.renderer.xr.addEventListener('sessionend', () => { this.preview.visible = true; this.arGroup.visible = false; this.reticle.visible = false; overlay.hidden = true; this.hitSource = null; });
    });
  }

  async #requestHitSource() {
    const session = this.renderer.xr.getSession();
    const viewer = await session.requestReferenceSpace('viewer');
    this.hitSource = await session.requestHitTestSource({ space: viewer });
  }

  #tick(frame) {
    if (frame && this.hitSource) {
      const hits = frame.getHitTestResults(this.hitSource);
      const pose = hits.length ? hits[0].getPose(this.renderer.xr.getReferenceSpace()) : null;
      this.reticle.visible = !!pose;
      if (pose) this.reticle.matrix.fromArray(pose.transform.matrix);
    } else {
      this.controls.update();
    }
    this.renderer.render(this.scene, this.camera);
  }

  #resize() {
    if (this.renderer.xr.isPresenting) return;
    const w = this.container.clientWidth, h = this.container.clientHeight;
    this.renderer.setSize(w, h, false);
    this.camera.aspect = w / h;
    this.camera.updateProjectionMatrix();
  }
}

function buildItems(layout) {
  const group = new THREE.Group();
  for (const p of layout.placements) {
    const h = p.type === 'MONITOR' ? 0.02 : Math.max(p.heightCm * CM, 0.01);
    const mesh = box(p.widthCm * CM, h, p.depthCm * CM, p.color);
    mesh.position.set(p.x * CM, h / 2, p.z * CM);
    group.add(mesh);
    if (p.type === 'MONITOR') {   // stand + screen so it reads as a monitor
      const screenW = (p.rotated ? p.depthCm : p.widthCm) * CM;
      const screen = box(screenW, 0.34, 0.02, 0x111827);
      screen.position.set(p.x * CM, p.heightCm * CM - 0.17, p.z * CM);
      const neck = box(0.04, p.heightCm * CM - 0.34, 0.03, 0x374151);
      neck.position.set(p.x * CM, (p.heightCm * CM - 0.34) / 2, p.z * CM);
      group.add(screen, neck);
    }
  }
  return group;
}

function box(w, h, d, color) {
  return new THREE.Mesh(new THREE.BoxGeometry(w, h, d), new THREE.MeshStandardMaterial({ color, roughness: 0.7 }));
}
