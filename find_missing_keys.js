const fs = require('fs');
const keys = new Set();
for (const file of ['src/pages/Expenses.tsx', 'src/pages/Debts.tsx']) {
  const content = fs.readFileSync(file, 'utf8');
  let m1 = [...content.matchAll(/t\(['"]([^'"]+)['"]\)/g)];
  m1.forEach(m => keys.add(m[1]));
}
const content = fs.readFileSync('src/contexts/SettingsContext.tsx', 'utf8');
let curKey = '';
let curDict = new Set();
const lines = content.split('\n');
for (const line of lines) {
  if (line.includes('en: {')) { curKey = 'en'; continue; }
  if (line.includes('bn: {')) { break; }
  if (curKey === 'en') {
    let k = line.trim().split(':')[0].trim();
    if(k) curDict.add(k);
  }
}
const missing = [...keys].filter(k => !curDict.has(k));
console.log('Missing EN keys:', missing);
