const fs = require('fs');

function doReplacements(file) {
    let content = fs.readFileSync(file, 'utf8');

    // Debts.tsx
    if (file.includes('Debts')) {
        content = content.replace('দেনা-পাওনার রিপোর্ট</p>', '${t(\'debtsReport\')}</p>');
        content = content.replace('বর্তমান ব্যালেন্স</p>', '${t(\'currentBalance\')}</p>');
        content = content.replace('রিপোর্ট তৈরির তারিখ:</p>', '${t(\'reportGenerationDate\')}:</p>');
        content = content.replace('>তারিখ</th>', '>${t(\'date\')}</th>');
        content = content.replace('>বিবরণ</th>', '>${t(\'description\')}</th>');
        content = content.replace('>পরিমাণ (৳)</th>', '>${t(\'amount\')} (৳)</th>');
        content = content.replace(/{t\('total'\)} {t\('got'\)}/g, '${t(\'total\')} ${t(\'got\')}');
        content = content.replace(/{t\('total'\)} {t\('gave'\)}/g, '${t(\'total\')} ${t(\'gave\')}');
    } 

    fs.writeFileSync(file, content, 'utf8');
}

doReplacements('src/pages/Debts.tsx');
