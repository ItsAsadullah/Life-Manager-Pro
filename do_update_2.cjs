const fs = require('fs');

function doReplacements() {
    // 1. SettingsContext
    let sc = fs.readFileSync('src/contexts/SettingsContext.tsx', 'utf8');

    // Add to EN (find 'items: \'Items\'' to insert after)
    // Wait, let's insert before 'items:'
    const enAdd = `    manageCategoriesTooltip: 'Manage Categories',
    viewGraph: 'View Graph',
    showSearchBox: 'Show Search Box',
    showMonthlyPercentage: 'Show Monthly Percentage',
    noTransactionsThisMonth: 'No transactions this month',
    manageCategoriesTitle: 'Manage Categories',
    newCategoryPlaceholder: 'New Category Name...',
    addBtn: 'Add',
    ledgerCalendar: 'Ledger Calendar',
    hasLedger: 'Has Ledger',
    noLedger: 'No Ledger',
    didNotCome: 'No Data',
    ledgerByCategory: 'Ledger by Category',
    other: 'Other',
    noDataFound: 'No Data Found',
    taka: 'Taka',
    categoryRatio: 'Category Ratio',
    `;
    sc = sc.replace("amount: 'Amount',", "amount: 'Amount',\n" + enAdd);

    // Add to BN
    const bnAdd = `    manageCategoriesTooltip: 'ক্যাটাগরি তৈরি/ডিলিট করুন',
    viewGraph: 'গ্রাফ দেখুন',
    showSearchBox: 'সার্চ বক্স দেখান',
    showMonthlyPercentage: 'মাসিক পার্সেন্টেজ দেখান',
    noTransactionsThisMonth: 'মাসে কোন লেনদেন নেই',
    manageCategoriesTitle: 'ক্যাটাগরি ম্যানেজ করুন',
    newCategoryPlaceholder: 'নতুন ক্যাটাগরির নাম...',
    addBtn: 'যোগ করুন',
    ledgerCalendar: 'হিসাব ক্যালেন্ডার',
    hasLedger: 'হিসাব আছে',
    noLedger: 'হিসাব নেই',
    didNotCome: 'আসেনি',
    ledgerByCategory: 'ক্যাটাগরি অনুযায়ী হিসাব',
    other: 'অন্যান্য',
    noDataFound: 'কোন তথ্য পাওয়া যায়নি',
    taka: 'টাকা',
    categoryRatio: 'ক্যাটাগরি অনুপাত',
    `;
    sc = sc.replace("amount: 'পরিমাণ',", "amount: 'পরিমাণ',\n" + bnAdd);

    fs.writeFileSync('src/contexts/SettingsContext.tsx', sc, 'utf8');

    // 2. Expenses.tsx
    let ex = fs.readFileSync('src/pages/Expenses.tsx', 'utf8');

    ex = ex.replace('ক্যাটাগরি তৈরি/ডিলিট করুন', '{t(\'manageCategoriesTooltip\')}');
    ex = ex.replace('গ্রাফ দেখুন', '{t(\'viewGraph\')}');
    ex = ex.replace('সার্চ বক্স দেখান', '{t(\'showSearchBox\')}');
    ex = ex.replace('মাসিক পার্সেন্টেজ দেখান', '{t(\'showMonthlyPercentage\')}');
    
    // Monthly No transaction
    ex = ex.replace('মাসে কোন লেনদেন নেই', '{t(\'noTransactionsThisMonth\')}');

    // Manage Categories Title
    ex = ex.replace('ক্যাটাগরি ম্যানেজ করুন', '{t(\'manageCategoriesTitle\')}');

    // newCategoryPlaceholder
    ex = ex.replace("placeholder={language === 'bn' ? 'নতুন ক্যাটাগরির নাম...' : 'New Category...'}", "placeholder={t('newCategoryPlaceholder')}");

    // Add button
    ex = ex.replace('যোগ করুন', '{t(\'addBtn\')}');

    // Ledger Calendar
    ex = ex.replace('হিসাব ক্যালেন্ডার', '{t(\'ledgerCalendar\')}');

    ex = ex.replace('হিসাব আছে', '{t(\'hasLedger\')}');
    ex = ex.replace('হিসাব নেই', '{t(\'noLedger\')}');
    ex = ex.replace('আসেনি', '{t(\'didNotCome\')}');

    // Ledger by Category
    ex = ex.replace('ক্যাটাগরি অনুযায়ী হিসাব', '{t(\'ledgerByCategory\')}');

    // 'অন্যান্য'
    ex = ex.replace(/'অন্যান্য'/g, "t('other')");
    
    // No Data
    ex = ex.replace(/কোন তথ্য পাওয়া যায়নি/g, "{t('noDataFound')}");

    // Taka
    ex = ex.replace(/>টাকা</g, ">{t('taka')}<");
    ex = ex.replace(/} টাকা/g, "} ${t('taka')}");

    // Category Ratio
    ex = ex.replace('ক্যাটাগরি অনুপাত', '{t(\'categoryRatio\')}');

    fs.writeFileSync('src/pages/Expenses.tsx', ex, 'utf8');

    // 3. Dashboard.tsx (Task 3: the PieChart uses #food in English but the label shows in lower case, which is fine as it's user generated data.)
    // Wait, the prompt said "the PieChart uses #food in English but the label shows in lower case, which is fine as it's user generated data."
    // That means explicitly NO ACTION needed for #food in Dashboard!
}

doReplacements();
console.log('Update Complete!');
