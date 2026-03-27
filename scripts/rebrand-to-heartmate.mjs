#!/usr/bin/env node
/**
 * Rebrand script: Room8 -> HeartMate
 * - Replaces visible brand text (Room8, ROOM8, room8) with HeartMate
 * - Updates logo references to /heartmate-logo.svg
 * - Updates HTML <title> to HeartMate
 * - Drops heartmate-logo.svg into discovered public directories
 *
 * Run: node scripts/rebrand-to-heartmate.mjs
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Directories to search for front-end code
const CANDIDATE_ROOTS = [
  '.',               // fallback to repo root
  'frontend',
  'client',
  'web',
  'ui',
  'app',
  'apps/web',
  'packages/web',
];
const EXCLUDE_DIRS = new Set([
  'node_modules', 'build', 'dist', 'out', '.git', '.idea', '.vscode',
  'target', 'bin', 'obj', 'coverage', '.next', '.cache', 'docs', 'backend'
]);

// File extensions to process
const TEXT_EXTS = new Set(['.js', '.jsx', '.ts', '.tsx', '.html', '.htm', '.css', '.scss', '.sass', '.mdx']);

// Replacement patterns
const BRAND_PATTERNS = [
  { re: /\bRoom8\b/g, replacement: 'HeartMate' },
  { re: /\bROOM8\b/g, replacement: 'HEARTMATE' },
  { re: /\broom8\b/g, replacement: 'heartmate' },
  { re: /\bRoom 8\b/g, replacement: 'HeartMate' },
];

// Replace common logo paths named for room8/roombay/etc. with the new HeartMate logo
const LOGO_RE = /(['"(])(?:\/)?(?:assets\/)?(?:images?\/)?(?:img\/)?(?:logos?\/)?(?:room8|room-?8|roombay|room-buddy|logo)(?:[-\w]*)\.(svg|png|jpg|jpeg|webp)(['")])/gi;
const LOGO_REPLACER = (_m, pre, _ext, post) => `${pre}/heartmate-logo.svg${post}`;

// SVG asset content
const HEARTMATE_SVG = `<?xml version="1.0" encoding="UTF-8"?>
<svg width="200" height="48" viewBox="0 0 200 48" fill="none" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="HeartMate Logo">
  <title>HeartMate</title>
  <desc>Heart icon with HeartMate wordmark</desc>
  <g transform="translate(4,4)">
    <path d="M20.5 36.5L7.8 24.6C3.1 20.2 3.2 12.6 8.1 8.4C12.6 4.6 18.5 6.1 20.9 10.1C23.3 6.2 29.2 4.7 33.7 8.4C38.6 12.6 38.7 20.2 34 24.6L21.3 36.5C21.1 36.7 20.7 36.7 20.5 36.5Z" fill="#E11D48"/>
  </g>
  <g transform="translate(52,30)">
    <text x="0" y="0" font-family="Inter, ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, Apple Color Emoji, Segoe UI Emoji" font-size="20" font-weight="700" fill="#111827" letter-spacing="0.5">
      HeartMate
    </text>
  </g>
</svg>
`;

const results = {
  filesScanned: 0,
  filesModified: 0,
  replacements: 0,
  logosInstalled: [],
  errors: [],
};

function shouldSkipDir(dir) {
  const name = path.basename(dir);
  return EXCLUDE_DIRS.has(name);
}

function isTextFile(file) {
  const ext = path.extname(file).toLowerCase();
  return TEXT_EXTS.has(ext);
}

function walk(dir, cb) {
  let entries;
  try {
    entries = fs.readdirSync(dir, { withFileTypes: true });
  } catch {
    return;
  }
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (!shouldSkipDir(full)) walk(full, cb);
    } else {
      cb(full);
    }
  }
}

function ensureHeartmateLogoInPublic(rootDir) {
  const publicDirCandidates = [
    path.join(rootDir, 'public'),
    path.join(rootDir, 'frontend', 'public'),
    path.join(rootDir, 'client', 'public'),
    path.join(rootDir, 'web', 'public'),
    path.join(rootDir, 'ui', 'public'),
    path.join(rootDir, 'app', 'public'),
  ];
  for (const pub of publicDirCandidates) {
    try {
      if (fs.existsSync(pub) && fs.statSync(pub).isDirectory()) {
        const logoPath = path.join(pub, 'heartmate-logo.svg');
        if (!fs.existsSync(logoPath)) {
          fs.writeFileSync(logoPath, HEARTMATE_SVG, 'utf8');
          results.logosInstalled.push(logoPath);
        }
      }
    } catch (e) {
      results.errors.push(`Failed to install logo in ${pub}: ${e.message}`);
    }
  }
}

function processFile(filePath) {
  if (!isTextFile(filePath)) return;

  let content;
  try {
    content = fs.readFileSync(filePath, 'utf8');
  } catch {
    return;
  }

  const original = content;
  let changed = false;

  // Brand text replacements
  for (const pat of BRAND_PATTERNS) {
    const before = content;
    content = content.replace(pat.re, pat.replacement);
    if (content !== before) {
      results.replacements += 1;
      changed = true;
    }
  }

  // Logo references
  {
    const before = content;
    content = content.replace(LOGO_RE, LOGO_REPLACER);
    if (content !== before) {
      results.replacements += 1;
      changed = true;
    }
  }

  // HTML title tags
  if (filePath.endsWith('.html') || filePath.endsWith('.htm')) {
    const before = content;
    content = content.replace(/<title>[\s\S]*?<\/title>/i, '<title>HeartMate</title>');
    if (content !== before) {
      results.replacements += 1;
      changed = true;
    }
  }

  results.filesScanned += 1;

  if (changed) {
    try {
      fs.writeFileSync(filePath, content, 'utf8');
      results.filesModified += 1;
    } catch (e) {
      results.errors.push(`Failed to write ${filePath}: ${e.message}`);
    }
  }
}

function main() {
  const repoRoot = path.resolve(__dirname, '..');
  const roots = CANDIDATE_ROOTS
    .map(p => path.resolve(repoRoot, p))
    .filter(p => fs.existsSync(p) && fs.statSync(p).isDirectory());

  if (roots.length === 0) {
    console.log('No candidate front-end roots found. Scanning repository root.');
    roots.push(repoRoot);
  }

  // Install logo into public folders
  ensureHeartmateLogoInPublic(repoRoot);
  for (const r of roots) ensureHeartmateLogoInPublic(r);

  // Scan and update files
  for (const root of roots) {
    walk(root, processFile);
  }

  // Summary
  console.log('Rebrand complete.');
  console.table({
    filesScanned: results.filesScanned,
    filesModified: results.filesModified,
    replacements: results.replacements,
    logosInstalled: results.logosInstalled.length,
    errors: results.errors.length,
  });
  if (results.logosInstalled.length) {
    console.log('Installed logos:', results.logosInstalled.join('\n - '));
  }
  if (results.errors.length) {
    console.warn('Errors encountered:');
    for (const e of results.errors) console.warn(' -', e);
  }
}

main();
