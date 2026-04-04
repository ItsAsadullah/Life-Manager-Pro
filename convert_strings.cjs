const fs = require('fs');
let content = fs.readFileSync('src/pages/Debts.tsx', 'utf8');

// Replace strings inside template literals with proper translation function calls
// Note: We use \${t('key')} so that it evaluates correctly during string interpolation.
content = content.replace('দেনা-পাওনার রিপোর্ট', '${t(\'debtsReport\')}');
content = content.replace('বর্তমান ব্যালেন্স', '${t(\'currentBalance\')}');
content = content.replace('রিপোর্ট তৈরির তারিখ:', '${t(\'reportGenerationDate\')}:');
content = content.replace('তারিখ</th>', '${t(\'date\')}</th>');
content = content.replace('বিবরণ</th>', '${t(\'description\')}</th>');
content = content.replace('পরিমাণ (৳)</th>', '${t(\'amount\')} (৳)</th>');

// Fix buggy {t('total')} {t('got')} in literal 
content = content.replace(/{t\('total'\)} {t\('got'\)}/g, '${t(\'total\')} ${t(\'got\')}');
content = content.replace(/{t\('total'\)} {t\('gave'\)}/g, '${t(\'total\')} ${t(\'gave\')}');

fs.writeFileSync('src/pages/Debts.tsx', content, 'utf8');
