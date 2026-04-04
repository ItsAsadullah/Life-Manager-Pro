
const fs = require("fs");
let code = fs.readFileSync("src/contexts/SettingsContext.tsx", "utf8");

const enInsert = `
    sinceLastMonth: "Since Last Month",
`;
const bnInsert = `
    sinceLastMonth: "গত মাস থেকে",
`;

code = code.replace(/en: \{/, "en: {" + enInsert);
code = code.replace(/bn: \{/, "bn: {" + bnInsert);

fs.writeFileSync("src/contexts/SettingsContext.tsx", code);

