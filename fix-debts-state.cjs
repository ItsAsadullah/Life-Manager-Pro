const fs = require('fs');

let fileStr = fs.readFileSync('src/pages/Debts.tsx', 'utf8');

fileStr = fileStr.replaceAll("'${t('cash') || 'Cash'}'", "t('cash') || 'Cash'");

fs.writeFileSync('src/pages/Debts.tsx', fileStr);
console.log("Fixed!");
