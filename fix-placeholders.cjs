const fs = require('fs');

let fileStr = fs.readFileSync('src/pages/Debts.tsx', 'utf8');

fileStr = fileStr.replaceAll('placeholder="${t(\'amount\') || \'Amount\'}"', "placeholder={t('amount') || 'Amount'}");
fileStr = fileStr.replaceAll('placeholder="${t(\'details\') || \'Details\'}"', "placeholder={t('details') || 'Details'}");
fileStr = fileStr.replaceAll('placeholder="${t(\'name\') || \'Name\'}"', "placeholder={t('name') || 'Name'}");
fileStr = fileStr.replaceAll('placeholder="${t(\'phoneOptional\') || \'Phone\'}"', "placeholder={t('phoneOptional') || 'Phone'}");
fileStr = fileStr.replaceAll('placeholder="${t(\'addressOptional\') || \'Address\'}"', "placeholder={t('addressOptional') || 'Address'}");

fs.writeFileSync('src/pages/Debts.tsx', fileStr);
console.log('Fixed placeholders');
