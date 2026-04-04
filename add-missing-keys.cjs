const fs = require('fs');

const missingEn = {
  searchPlaceholder: 'Search by name or phone...',
  amount: 'Amount',
  details: 'Details (Optional)',
  name: 'Name',
  phoneOptional: 'Phone number (Optional)',
  addressOptional: 'Address (Optional)',
  balanceIsEqual: 'Balance Even',
  confirmDeleteTransaction: 'Delete transaction?',
  noTransaction: 'No transactions found',
  personDetails: 'Person Details',
  paymentCash: 'Cash',
  paymentBkash: 'bKash',
  paymentNagad: 'Nagad',
  paymentBank: 'Bank',
  errorGeneratingPDF: 'Error generating PDF',
  addTransaction: 'Add Transaction',
  confirmDelete: 'Are you sure you want to delete this?'
};

const missingBn = {
  searchPlaceholder: 'নাম বা ফোন নম্বর দিয়ে খুঁজুন...',
  amount: 'টাকার পরিমাণ',
  details: 'বিবরণ (ঐচ্ছিক)',
  name: 'নাম',
  phoneOptional: 'ফোন (ঐচ্ছিক)',
  addressOptional: 'ঠিকানা (ঐচ্ছিক)',
  balanceIsEqual: 'ব্যালেন্স সমান',
  confirmDeleteTransaction: 'এই লেনদেনটি ডিলিট করতে চান?',
  noTransaction: 'কোনো লেনদেন পাওয়া যায়নি',
  personDetails: 'ব্যক্তির বিবরণ',
  paymentCash: 'নগদ ক্যাশ',
  paymentBkash: 'বিকাশ',
  paymentNagad: 'নগদ একাউন্ট',
  paymentBank: 'ব্যাংক',
  errorGeneratingPDF: 'PDF তৈরি করতে সমস্যা হয়েছে',
  addTransaction: 'লেনদেন যুক্ত করুন',
  confirmDelete: 'আপনি কি নিশ্চিত যে এটি ডিলিট করতে চান?'
};

let settingsStr = fs.readFileSync('src/contexts/SettingsContext.tsx', 'utf8');

const lines = settingsStr.split('\n');
let newLines = [];
let inBn = false;

for (let i = 0; i < lines.length; i++) {
  const line = lines[i];
  
  if (line.includes('bn: {')) {
    for (const [k, v] of Object.entries(missingEn)) {
      if (!settingsStr.includes(k + ':')) {
        newLines.push(`      ${k}: '${v.replace(/'/g, "\\'")}',`);
      }
    }
  }
  
  if (line.match(/^\s*}\s*$/) && lines[i+1] && lines[i+1].match(/^\s*};\s*$/)) {
    for (const [k, v] of Object.entries(missingBn)) {
      if (!settingsStr.includes(k + ':')) {
        newLines.push(`      ${k}: '${v.replace(/'/g, "\\'")}',`);
      }
    }
  }
  
  newLines.push(line);
}

fs.writeFileSync('src/contexts/SettingsContext.tsx', newLines.join('\n'));
console.log('Appended missing keys globally.');