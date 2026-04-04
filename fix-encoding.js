const fs = require('fs');
let code = fs.readFileSync('src/pages/Debts.tsx', 'utf8');

// The file might be corrupted with mojibake if opened in incorrect encoding and saved, 
// OR maybe the encoding is correct but reading in powershell printed mojibake. 
// Let's print out the exact text blocks from the file to check.

console.log(code.match(/<div className="flex justify-between items-center mb-1">[\s\S]*?<\/div>/g));

