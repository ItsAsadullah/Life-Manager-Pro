const fs = require('fs');

let fileStr = fs.readFileSync('src/pages/Debts.tsx', 'utf8');

const replacements = {
  // Stats
  'পাবো': `{t('willGet')}`,
  'দিবো': `{t('willGive')}`,
  'মোট': `{t('total')}`,
  'জন': `{t('people')}`,
  'নাম বা ফোন নম্বর দিয়ে খুঁজুন...': `\${t('searchPlaceholder') || 'Search name/phone...'}`,
  'কোনো ব্যক্তি পাওয়া যায়নি': `{t('noPersonFound')}`,
  
  // Person views
  'ব্যক্তির বিবরণ': `{t('personDetails') || 'Person Details'}`,
  'পেলাম': `{t('got') || 'Got'}`,
  'দিলাম': `{t('gave') || 'Gave'}`,
  'এডিট': `{t('edit') || 'Edit'}`,
  'ডিলেট': `{t('delete') || 'Delete'}`,
  'কোনো লেনদেন পাওয়া যায়নি': `{t('noTransaction') || 'No transactions'}`,
  'লেনদেন অ্যাড করুন': `{t('addTransaction') || 'Add Transaction'}`,
  'টাকার পরিমাণ': `\${t('amount') || 'Amount'}`,
  'বিবরণ (ঐচ্ছিক)': `\${t('details') || 'Details'}`,
  'ক্যাশ': `\${t('cash') || 'Cash'}`,
  'বিকাশ': `\${t('bkash') || 'bKash'}`,
  'নগদ': `\${t('nagad') || 'Nagad'}`,
  'ব্যাংক একাউন্ট': `\${t('bank') || 'Bank'}`,
  'ব্যাংক': `\${t('bank') || 'Bank'}`,
  'সেভ': `{t('save') || 'Save'}`,
  'সেভ করুন': `{t('save') || 'Save'}`,
  'নাম': `\${t('name') || 'Name'}`,
  'ফোন নম্বর (ঐচ্ছিক)': `\${t('phoneOptional') || 'Phone'}`,
  'ঠিকানা (ঐচ্ছিক)': `\${t('addressOptional') || 'Address'}`,
  'আপনি কি নিশ্চিত যে এটি ডিলিট করতে চান?': `\${t('confirmDelete') || 'Are you sure?'}`,
  'আপনি কি এই লেনদেন ডিলিট করতে চান?': `\${t('confirmDeleteTransaction') || 'Delete transaction?'}`,
  'ব্যালেন্স সমান': `{t('balanceIsEqual') || 'Balance Even'}`
};

for (const [key, value] of Object.entries(replacements)) {
  const isAttr = value.startsWith('${') || value.startsWith('{t');
  if (value.startsWith('${')) {
     fileStr = fileStr.replaceAll(key, value);
  } else {
     fileStr = fileStr.replaceAll(key, value.replace('{t', "{t").replace('}', "}"));
  }
}

fs.writeFileSync('src/pages/Debts.tsx.new', fileStr);
