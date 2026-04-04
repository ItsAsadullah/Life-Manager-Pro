const fs = require('fs');

// Fix Debts.tsx
let fileStr = fs.readFileSync('src/pages/Debts.tsx', 'utf8');

fileStr = fileStr.replaceAll("window.confirm(`${t('confirmDeleteTransaction') || 'Delete transaction?'}`", "window.confirm(t('confirmDeleteTransaction') || 'Delete transaction?'");
fileStr = fileStr.replaceAll("window.confirm('${t('confirmDeleteTransaction') || 'Delete transaction?'}'", "window.confirm(t('confirmDeleteTransaction') || 'Delete transaction?'");
fileStr = fileStr.replaceAll("window.confirm(`${t('confirmDelete') || 'Are you sure?'}`", "window.confirm(t('confirmDelete') || 'Are you sure?'");
fileStr = fileStr.replaceAll("window.confirm('${t('confirmDelete') || 'Are you sure?'}'", "window.confirm(t('confirmDelete') || 'Are you sure?'");

fs.writeFileSync('src/pages/Debts.tsx', fileStr);

// Fix SettingsContext.tsx duplicate keys
let settingsStr = fs.readFileSync('src/contexts/SettingsContext.tsx', 'utf8');
const lines = settingsStr.split('\n');
const fixedLines = [];
let inEn = false;
let inBn = false;
const seenEnKeys = new Set();
const seenBnKeys = new Set();

for (const line of lines) {
  if (line.includes('const en = {')) { inEn = true; fixedLines.push(line); continue; }
  if (line.includes('const bn = {')) { inEn = false; inBn = true; fixedLines.push(line); continue; }
  if (line.match(/^\s*};/)) { inEn = false; inBn = false; fixedLines.push(line); continue; }

  const match = line.match(/^\s*'?([a-zA-Z0-9_]+)'?\s*:/);
  if (match) {
    const key = match[1];
    if (inEn) {
      if (seenEnKeys.has(key)) continue;
      seenEnKeys.add(key);
    } else if (inBn) {
      if (seenBnKeys.has(key)) continue;
      seenBnKeys.add(key);
    }
  }
  fixedLines.push(line);
}

fs.writeFileSync('src/contexts/SettingsContext.tsx', fixedLines.join('\n'));
console.log('Fixed window.confirm and duplicate keys');
