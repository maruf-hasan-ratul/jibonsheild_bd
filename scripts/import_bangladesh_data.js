/**
 * JibonShield BD - Complete Bangladesh Administrative & Emergency Dataset Import Script
 * 
 * Imports 8 Divisions, 64 Districts, 495 Upazilas, Designated Shelters,
 * and Official Disaster Updates into Firebase Cloud Firestore.
 * 
 * Usage:
 * 1. Place your Firebase service account private key as 'serviceAccountKey.json' in this folder
 *    (or set the GOOGLE_APPLICATION_CREDENTIALS environment variable).
 * 2. Run: npm install
 * 3. Run: node import_bangladesh_data.js
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const serviceAccountPath = path.join(__dirname, 'serviceAccountKey.json');

if (fs.existsSync(serviceAccountPath)) {
    const serviceAccount = require(serviceAccountPath);
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        projectId: 'jibonsheildbd'
    });
    console.log('Initialized Firebase Admin with serviceAccountKey.json');
} else if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    admin.initializeApp({
        credential: admin.credential.applicationDefault(),
        projectId: 'jibonsheildbd'
    });
    console.log('Initialized Firebase Admin using GOOGLE_APPLICATION_CREDENTIALS');
} else {
    try {
        admin.initializeApp({
            projectId: 'jibonsheildbd'
        });
        console.log('Initialized Firebase Admin with default credentials (projectId: jibonsheildbd)');
    } catch (e) {
        console.error('ERROR: Could not find serviceAccountKey.json or Google application credentials.');
        console.error('Please download a service account JSON from Firebase Console > Project Settings > Service accounts,');
        console.error('and save it as "scripts/serviceAccountKey.json".');
        process.exit(1);
    }
}

const db = admin.firestore();
const FieldValue = admin.firestore.FieldValue;

// Helper to write items in batches <= 400
async function commitBatch(collectionName, items, idKey = 'id') {
    const BATCH_SIZE = 400;
    let batch = db.batch();
    let count = 0;
    let total = 0;

    for (const item of items) {
        const docId = item[idKey] || item.id;
        const docRef = db.collection(collectionName).doc(docId);
        
        const data = { ...item };
        delete data[idKey];
        data.updatedAt = FieldValue.serverTimestamp();
        if (!data.createdAt) {
            data.createdAt = FieldValue.serverTimestamp();
        }

        batch.set(docRef, data, { merge: true });
        count++;
        total++;

        if (count >= BATCH_SIZE) {
            await batch.commit();
            console.log(`Committed ${total} / ${items.length} records into ${collectionName}`);
            batch = db.batch();
            count = 0;
        }
    }

    if (count > 0) {
        await batch.commit();
        console.log(`Finished committing ${total} records into ${collectionName}`);
    }
}

// -------------------------------------------------------------
// 1. DIVISIONS
// -------------------------------------------------------------
const divisions = [
    { id: 'dhaka', name: 'Dhaka', nameBn: 'ঢাকা', code: 'DHAKA', isActive: true },
    { id: 'chattogram', name: 'Chattogram', nameBn: 'চট্টগ্রাম', code: 'CTG', isActive: true },
    { id: 'rajshahi', name: 'Rajshahi', nameBn: 'রাজশাহী', code: 'RAJ', isActive: true },
    { id: 'khulna', name: 'Khulna', nameBn: 'খুলনা', code: 'KHU', isActive: true },
    { id: 'barishal', name: 'Barishal', nameBn: 'বরিশাল', code: 'BAR', isActive: true },
    { id: 'sylhet', name: 'Sylhet', nameBn: 'সিলেট', code: 'SYL', isActive: true },
    { id: 'rangpur', name: 'Rangpur', nameBn: 'রংপুর', code: 'RAN', isActive: true },
    { id: 'mymensingh', name: 'Mymensingh', nameBn: 'ময়মনসিংহ', code: 'MYM', isActive: true }
];

// -------------------------------------------------------------
// 2. DISTRICTS & 3. UPAZILAS DATA
// -------------------------------------------------------------
const divisionDistricts = {
    dhaka: [
        { id: 'dhaka', name: 'Dhaka', nameBn: 'ঢাকা', code: 'DHA', upazilas: ['Dhamrai', 'Dohar', 'Keraniganj', 'Nawabganj', 'Savar', 'Tejgaon Circle'] },
        { id: 'gazipur', name: 'Gazipur', nameBn: 'গাজীপুর', code: 'GAZ', upazilas: ['Gazipur Sadar', 'Kaliakair', 'Kaliganj', 'Kapasia', 'Sreepur'] },
        { id: 'narayanganj', name: 'Narayanganj', nameBn: 'নারায়ণগঞ্জ', code: 'NAR', upazilas: ['Araihazar', 'Bandar', 'Narayanganj Sadar', 'Rupganj', 'Sonargaon'] },
        { id: 'tangail', name: 'Tangail', nameBn: 'টাঙ্গাইল', code: 'TAN', upazilas: ['Tangail Sadar', 'Basail', 'Bhuapur', 'Delduar', 'Dhanbari', 'Ghatail', 'Gopalpur', 'Kalihati', 'Madhupur', 'Mirzapur', 'Nagarpur', 'Sakhipur'] },
        { id: 'narsingdi', name: 'Narsingdi', nameBn: 'নরসিংদী', code: 'NSD', upazilas: ['Narsingdi Sadar', 'Belabo', 'Monohardi', 'Palash', 'Raipura', 'Shibpur'] },
        { id: 'faridpur', name: 'Faridpur', nameBn: 'ফরিদপুর', code: 'FAR', upazilas: ['Faridpur Sadar', 'Alfadanga', 'Bhanga', 'Boalmari', 'Charbhadrasan', 'Madhukhali', 'Nagarkanda', 'Sadarpur', 'Saltha'] },
        { id: 'gopalganj', name: 'Gopalganj', nameBn: 'গোপালগঞ্জ', code: 'GOP', upazilas: ['Gopalganj Sadar', 'Kashiani', 'Kotalipara', 'Muksudpur', 'Tungipara'] },
        { id: 'kishoreganj', name: 'Kishoreganj', nameBn: 'কিশোরগঞ্জ', code: 'KIS', upazilas: ['Kishoreganj Sadar', 'Astagram', 'Bajitpur', 'Bhairab', 'Hossainpur', 'Itna', 'Karimganj', 'Katiadi', 'Kuliarchar', 'Mithamain', 'Nikli', 'Pakundia', 'Tarail'] },
        { id: 'madaripur', name: 'Madaripur', nameBn: 'মাদারীপুর', code: 'MAD', upazilas: ['Madaripur Sadar', 'Dasar', 'Kalkini', 'Rajoir', 'Shibchar'] },
        { id: 'manikganj', name: 'Manikganj', nameBn: 'মানিকগঞ্জ', code: 'MAN', upazilas: ['Manikganj Sadar', 'Daulatpur', 'Ghior', 'Harirampur', 'Saturia', 'Shivalaya', 'Singair'] },
        { id: 'munshiganj', name: 'Munshiganj', nameBn: 'মুন্সীগঞ্জ', code: 'MUN', upazilas: ['Munshiganj Sadar', 'Gazaria', 'Louhajang', 'Sirajdikhan', 'Sreenagar', 'Tongibari'] },
        { id: 'rajbari', name: 'Rajbari', nameBn: 'রাজবাড়ী', code: 'RAJ_DIST', upazilas: ['Rajbari Sadar', 'Baliakandi', 'Goalandaghat', 'Kalukhali', 'Pangsha'] },
        { id: 'shariatpur', name: 'Shariatpur', nameBn: 'শরীয়তপুর', code: 'SHA', upazilas: ['Shariatpur Sadar', 'Bhedarganj', 'Damudya', 'Gosairhat', 'Naria', 'Zanjira'] }
    ],
    chattogram: [
        { id: 'chittagong', name: 'Chittagong', nameBn: 'চট্টগ্রাম', code: 'CTG_DIST', upazilas: ['Anwara', 'Banshkhali', 'Boalkhali', 'Chandanaish', 'Fatikchhari', 'Hathazari', 'Karnaphuli', 'Lohagara', 'Mirsharai', 'Patiya', 'Rangunia', 'Raozan', 'Sandwip', 'Satkania', 'Sitakunda'] },
        { id: 'coxs_bazar', name: "Cox's Bazar", nameBn: "কক্সবাজার", code: 'COX', upazilas: ['Chakaria', "Cox's Bazar Sadar", 'Eidgaon', 'Kutubdia', 'Maheshkhali', 'Pekua', 'Ramu', 'Teknaf', 'Ukhia'] },
        { id: 'comilla', name: 'Comilla', nameBn: 'কুমিল্লা', code: 'COM', upazilas: ['Barura', 'Brahmanpara', 'Burichang', 'Chandina', 'Chauddagram', 'Cumilla Adarsha Sadar', 'Cumilla Sadar Dakshin', 'Daudkandi', 'Debidwar', 'Homna', 'Laksam', 'Lalmai', 'Meghna', 'Monohargonj', 'Muradnagar', 'Nangalkot', 'Titas'] },
        { id: 'feni', name: 'Feni', nameBn: 'ফেনী', code: 'FEN', upazilas: ['Feni Sadar', 'Chhagalnaiya', 'Daganbhuiyan', 'Fulgazi', 'Parshuram', 'Sonagazi'] },
        { id: 'brahmanbaria', name: 'Brahmanbaria', nameBn: 'ব্রাহ্মণবাড়িয়া', code: 'BRA', upazilas: ['Brahmanbaria Sadar', 'Akhaura', 'Ashuganj', 'Bancharampur', 'Bijoynagar', 'Kasba', 'Nabinagar', 'Nasirnagar', 'Sarail'] },
        { id: 'chandpur', name: 'Chandpur', nameBn: 'চাঁদপুর', code: 'CHN', upazilas: ['Chandpur Sadar', 'Faridganj', 'Haimchar', 'Hajiganj', 'Kachua', 'Matlab North', 'Matlab South', 'Shahrasti'] },
        { id: 'lakshmipur', name: 'Lakshmipur', nameBn: 'লক্ষ্মীপুর', code: 'LAK', upazilas: ['Lakshmipur Sadar', 'Kamalnagar', 'Raipur', 'Ramganj', 'Ramgati'] },
        { id: 'noakhali', name: 'Noakhali', nameBn: 'নোয়াখালী', code: 'NOA', upazilas: ['Noakhali Sadar', 'Begumganj', 'Chatkhil', 'Companiganj', 'Hatiya', 'Kabirhat', 'Senbagh', 'Sonaimuri', 'Subarnachar'] },
        { id: 'khagrachhari', name: 'Khagrachhari', nameBn: 'খাগড়াছড়ি', code: 'KHA', upazilas: ['Khagrachhari Sadar', 'Dighinala', 'Guimara', 'Laksmichhari', 'Mahalchhari', 'Manikchhari', 'Matiranga', 'Panchhari', 'Ramgarh'] },
        { id: 'rangamati', name: 'Rangamati', nameBn: 'রাঙ্গামাটি', code: 'RAN_DIST', upazilas: ['Rangamati Sadar', 'Bagaichhari', 'Barkal', 'Belaichhari', 'Juraichhari', 'Kaptai', 'Kawkhali', 'Langadu', 'Naniarchar', 'Rajasthali'] },
        { id: 'bandarban', name: 'Bandarban', nameBn: 'বান্দরবান', code: 'BAN', upazilas: ['Bandarban Sadar', 'Alikadam', 'Lama', 'Naikhongchhari', 'Rowangchhari', 'Ruma', 'Thanchi'] }
    ],
    sylhet: [
        { id: 'sylhet', name: 'Sylhet', nameBn: 'সিলেট', code: 'SYL_DIST', upazilas: ['Sylhet Sadar', 'Balaganj', 'Beanibazar', 'Bishwanath', 'Companiganj', 'Dakshin Surma', 'Fenchuganj', 'Golapganj', 'Gowainghat', 'Jaintiapur', 'Kanaighat', 'Osmani Nagar', 'Zakiganj'] },
        { id: 'moulvibazar', name: 'Moulvibazar', nameBn: 'মৌলভীবাজার', code: 'MOU', upazilas: ['Moulvibazar Sadar', 'Barlekha', 'Juri', 'Kamalganj', 'Kulaura', 'Rajnagar', 'Sreemangal'] },
        { id: 'habiganj', name: 'Habiganj', nameBn: 'হবিগঞ্জ', code: 'HAB', upazilas: ['Habiganj Sadar', 'Ajmiriganj', 'Bahubal', 'Baniyachong', 'Chunarughat', 'Lakhai', 'Madhabpur', 'Nabiganj', 'Sayestaganj'] },
        { id: 'sunamganj', name: 'Sunamganj', nameBn: 'সুনামগঞ্জ', code: 'SUN', upazilas: ['Sunamganj Sadar', 'Bishwamvarpur', 'Chhatak', 'Derai', 'Dharampasha', 'Dowarabazar', 'Jagannathpur', 'Jamalganj', 'Madhyanagar', 'Shantiganj', 'Sullah', 'Tahirpur'] }
    ],
    rajshahi: [
        { id: 'rajshahi', name: 'Rajshahi', nameBn: 'রাজশাহী', code: 'RAJ_DIST', upazilas: ['Bagha', 'Bagmara', 'Charghat', 'Durgapur', 'Godagari', 'Mohanpur', 'Paba', 'Puthia', 'Tanore'] },
        { id: 'bogura', name: 'Bogura', nameBn: 'বগুড়া', code: 'BOG', upazilas: ['Bogura Sadar', 'Adamdighi', 'Dhunat', 'Dhupchanchia', 'Gabtali', 'Kahaloo', 'Nandigram', 'Sariakandi', 'Shajahanpur', 'Sherpur', 'Shibganj', 'Sonatola'] },
        { id: 'joypurhat', name: 'Joypurhat', nameBn: 'জয়পুরহাট', code: 'JOY', upazilas: ['Joypurhat Sadar', 'Akkelpur', 'Kalai', 'Khetlal', 'Panchbibi'] },
        { id: 'naogaon', name: 'Naogaon', nameBn: 'নওগাঁ', code: 'NAO', upazilas: ['Naogaon Sadar', 'Atrai', 'Badalgachhi', 'Dhamoirhat', 'Manda', 'Mohadevpur', 'Niamatpur', 'Patnitala', 'Porsha', 'Raninagar', 'Sapahar'] },
        { id: 'natore', name: 'Natore', nameBn: 'নাটোর', code: 'NAT', upazilas: ['Natore Sadar', 'Bagatipara', 'Baraigram', 'Gurudaspur', 'Lalpur', 'Naldanga', 'Singra'] },
        { id: 'chapainawabganj', name: 'Chapainawabganj', nameBn: 'চাঁপাইনবাবগঞ্জ', code: 'CHA', upazilas: ['Chapainawabganj Sadar', 'Bholahat', 'Gomastapur', 'Nachole', 'Shibganj'] },
        { id: 'pabna', name: 'Pabna', nameBn: 'পাবনা', code: 'PAB', upazilas: ['Pabna Sadar', 'Atgharia', 'Bera', 'Bhangura', 'Chatmohar', 'Faridpur', 'Ishwardi', 'Santhia', 'Sujanagar'] },
        { id: 'sirajganj', name: 'Sirajganj', nameBn: 'সিরাজগঞ্জ', code: 'SIR', upazilas: ['Sirajganj Sadar', 'Belkuchi', 'Chauhali', 'Kamarkhanda', 'Kazipur', 'Raiganj', 'Shahjadpur', 'Tarash', 'Ullapara'] }
    ],
    rangpur: [
        { id: 'rangpur', name: 'Rangpur', nameBn: 'রংপুর', code: 'RAN_DIST', upazilas: ['Rangpur Sadar', 'Badarganj', 'Gangachhara', 'Kaunia', 'Mithapukur', 'Pirgachha', 'Pirganj', 'Taraganj'] },
        { id: 'dinajpur', name: 'Dinajpur', nameBn: 'দিনাজপুর', code: 'DIN', upazilas: ['Dinajpur Sadar', 'Birampur', 'Birganj', 'Birol', 'Bochaganj', 'Chirirbandar', 'Fulbari', 'Ghoraghat', 'Hakimpur', 'Kaharole', 'Khansama', 'Nawabganj', 'Parbatipur'] },
        { id: 'gaibandha', name: 'Gaibandha', nameBn: 'গাইবান্ধা', code: 'GAI', upazilas: ['Gaibandha Sadar', 'Fulchhari', 'Gobindaganj', 'Palashbari', 'Sadullapur', 'Sughatta', 'Sundarganj'] },
        { id: 'kurigram', name: 'Kurigram', nameBn: 'কুড়িগ্রাম', code: 'KUR', upazilas: ['Kurigram Sadar', 'Bhurungamari', 'Char Rajibpur', 'Chilmari', 'Nageshwari', 'Phulbari', 'Rajarhat', 'Roumari', 'Ulipur'] },
        { id: 'lalmonirhat', name: 'Lalmonirhat', nameBn: 'লালমনিরহাট', code: 'LAL', upazilas: ['Lalmonirhat Sadar', 'Aditmari', 'Hatibandha', 'Kaliganj', 'Patgram'] },
        { id: 'nilphamari', name: 'Nilphamari', nameBn: 'নীলফামারী', code: 'NIL', upazilas: ['Nilphamari Sadar', 'Dimla', 'Domar', 'Jaldhaka', 'Kishoreganj', 'Syedpur'] },
        { id: 'panchagarh', name: 'Panchagarh', nameBn: 'পঞ্চগড়', code: 'PAN', upazilas: ['Panchagarh Sadar', 'Atwari', 'Boda', 'Debiganj', 'Tetulia'] },
        { id: 'thakurgaon', name: 'Thakurgaon', nameBn: 'ঠাকুরগাঁও', code: 'THA', upazilas: ['Thakurgaon Sadar', 'Baliadangi', 'Haripur', 'Pirganj', 'Ranisankail'] }
    ],
    khulna: [
        { id: 'khulna', name: 'Khulna', nameBn: 'খুলনা', code: 'KHU_DIST', upazilas: ['Batiaghata', 'Dacope', 'Dighalia', 'Dumuria', 'Koyra', 'Paikgachha', 'Phultala', 'Rupsha', 'Terokhada'] },
        { id: 'bagerhat', name: 'Bagerhat', nameBn: 'বাগেরহাট', code: 'BAG', upazilas: ['Bagerhat Sadar', 'Chitalmari', 'Fakirhat', 'Kachua', 'Mollahat', 'Mongla', 'Morrelganj', 'Rampal', 'Sarankhola'] },
        { id: 'chuadanga', name: 'Chuadanga', nameBn: 'চুয়াডাঙ্গা', code: 'CHU', upazilas: ['Chuadanga Sadar', 'Alamdanga', 'Damurhuda', 'Jibannagar'] },
        { id: 'jashore', name: 'Jashore', nameBn: 'যশোর', code: 'JAS', upazilas: ['Jashore Sadar', 'Abhaynagar', 'Bagherpara', 'Chaugachha', 'Jhikargachha', 'Keshabpur', 'Manirampur', 'Sharsha'] },
        { id: 'jhenaidah', name: 'Jhenaidah', nameBn: 'ঝিনাইদহ', code: 'JHE', upazilas: ['Jhenaidah Sadar', 'Harinakundu', 'Kaliganj', 'Kotchandpur', 'Maheshpur', 'Shailkupa'] },
        { id: 'kushtia', name: 'Kushtia', nameBn: 'কুষ্টিয়া', code: 'KUS', upazilas: ['Kushtia Sadar', 'Bheramara', 'Daulatpur', 'Khoksa', 'Kumarkhali', 'Mirpur'] },
        { id: 'magura', name: 'Magura', nameBn: 'মাগুরা', code: 'MAG', upazilas: ['Magura Sadar', 'Mohammadpur', 'Shalikha', 'Sreepur'] },
        { id: 'meherpur', name: 'Meherpur', nameBn: 'মেহেরপুর', code: 'MEH', upazilas: ['Meherpur Sadar', 'Gangni', 'Mujibnagar'] },
        { id: 'narail', name: 'Narail', nameBn: 'নড়াইল', code: 'NAR_DIST', upazilas: ['Narail Sadar', 'Kalia', 'Lohagara'] },
        { id: 'satkhira', name: 'Satkhira', nameBn: 'সাতক্ষীরা', code: 'SAT', upazilas: ['Satkhira Sadar', 'Assasuni', 'Debhata', 'Kalaroa', 'Kaliganj', 'Shyamnagar', 'Tala'] }
    ],
    barishal: [
        { id: 'barisal', name: 'Barisal', nameBn: 'বরিশাল', code: 'BAR_DIST', upazilas: ['Barisal Sadar', 'Agailjhara', 'Babuganj', 'Bakerganj', 'Banaripara', 'Gaurnadi', 'Hizla', 'Mehendiganj', 'Muladi', 'Wazirpur'] },
        { id: 'barguna', name: 'Barguna', nameBn: 'বরগুনা', code: 'BARG', upazilas: ['Barguna Sadar', 'Amtali', 'Bamna', 'Betagi', 'Patharghata', 'Taltali'] },
        { id: 'bhola', name: 'Bhola', nameBn: 'ভোলা', code: 'BHO', upazilas: ['Bhola Sadar', 'Borhanuddin', 'Char Fasson', 'Daulatkhan', 'Lalmohan', 'Manpura', 'Tazumuddin'] },
        { id: 'jhalokati', name: 'Jhalokati', nameBn: 'ঝালকাঠি', code: 'JHA', upazilas: ['Jhalokati Sadar', 'Kathalia', 'Nalchity', 'Rajapur'] },
        { id: 'patuakhali', name: 'Patuakhali', nameBn: 'পটুয়াখালী', code: 'PAT', upazilas: ['Patuakhali Sadar', 'Bauphal', 'Dashmina', 'Dumki', 'Galachipa', 'Kalapara', 'Mirzaganj', 'Rangabali'] },
        { id: 'pirojpur', name: 'Pirojpur', nameBn: 'পিরোজপুর', code: 'PIR', upazilas: ['Pirojpur Sadar', 'Bhandaria', 'Indurkani', 'Kawkhali', 'Mathbaria', 'Nazirpur', 'Nesarabad'] }
    ],
    mymensingh: [
        { id: 'mymensingh', name: 'Mymensingh', nameBn: 'ময়মনসিংহ', code: 'MYM_DIST', upazilas: ['Mymensingh Sadar', 'Bhaluka', 'Dhobaura', 'Fulbaria', 'Gaffargaon', 'Gauripur', 'Haluaghat', 'Ishwarganj', 'Muktagachha', 'Nandail', 'Phulpur', 'Tara Khanda', 'Trishal'] },
        { id: 'jamalpur', name: 'Jamalpur', nameBn: 'জামালপুর', code: 'JAM', upazilas: ['Jamalpur Sadar', 'Bakshiganj', 'Dewanganj', 'Islampur', 'Madarganj', 'Melandaha', 'Sarishabari'] },
        { id: 'netrokona', name: 'Netrokona', nameBn: 'নেত্রকোণা', code: 'NET', upazilas: ['Netrokona Sadar', 'Atpara', 'Barhatta', 'Durgapur', 'Kalmakanda', 'Kendua', 'Khaliajuri', 'Madan', 'Mohanganj', 'Purbadhala'] },
        { id: 'sherpur', name: 'Sherpur', nameBn: 'শেরপুর', code: 'SHE', upazilas: ['Sherpur Sadar', 'Jhenaigati', 'Nakla', 'Nalitabari', 'Sreebardi'] }
    ]
};

// Flatten districts and upazilas
const districts = [];
const upazilas = [];

for (const [divisionId, distList] of Object.entries(divisionDistricts)) {
    for (const d of distList) {
        districts.push({
            id: d.id,
            name: d.name,
            nameBn: d.nameBn,
            divisionId: divisionId,
            code: d.code,
            isActive: true
        });

        for (const uName of d.upazilas) {
            const upazilaId = uName.toLowerCase().replace(/[^a-z0-9]/g, '_').replace(/_+/g, '_');
            upazilas.push({
                id: `${d.id}_${upazilaId}`,
                name: uName,
                nameBn: uName,
                districtId: d.id,
                code: upazilaId.toUpperCase(),
                isActive: true
            });
        }
    }
}

// -------------------------------------------------------------
// 4. EMERGENCY SHELTERS
// -------------------------------------------------------------
const shelters = [
    {
        id: 'khulna_shelter_001',
        name: 'Khulna Government Cyclone Shelter',
        nameBn: 'খুলনা সরকারি সাইক্লোন আশ্রয়কেন্দ্র',
        description: 'Multi-purpose high-elevation refuge center equipped with solar backup.',
        descriptionBn: 'সৌর বিদ্যুৎ সমৃদ্ধ বহুমুখী উঁচু আশ্রয়কেন্দ্র।',
        divisionId: 'khulna',
        districtId: 'khulna',
        upazilaId: 'dumuria',
        address: 'Dumuria, Khulna, Bangladesh',
        addressBn: 'ডুমুরিয়া, খুলনা, বাংলাদেশ',
        latitude: 22.8150,
        longitude: 89.5500,
        capacity: 650,
        currentOccupancy: 80,
        shelterType: 'Cyclone Shelter',
        facilities: ['Solar Electricity', 'Drinking Water', 'First Aid', 'Sanitation', 'Livestock Pen'],
        contactNumber: '01711000001',
        isActive: true
    },
    {
        id: 'cox_shelter_001',
        name: 'Teknaf Coastal Multi-Purpose Shelter',
        nameBn: 'টেকনাফ উপকূলীয় বহুমুখী আশ্রয়কেন্দ্র',
        description: 'Designated high ground fortress with medical bay.',
        descriptionBn: 'উঁচু উপকূলীয় আশ্রয়কেন্দ্র ও জরুরি প্রাথমিক চিকিৎসা ব্যবস্থা।',
        divisionId: 'chattogram',
        districtId: 'coxs_bazar',
        upazilaId: 'teknaf',
        address: 'Teknaf Coastal Road, Cox\'s Bazar',
        addressBn: 'টেকনাফ উপকূলীয় সড়ক, কক্সবাজার',
        latitude: 20.8650,
        longitude: 92.2980,
        capacity: 1200,
        currentOccupancy: 0,
        shelterType: 'Cyclone Shelter',
        facilities: ['Drinking Water', 'Medical Bay', 'Women Safe Space', 'Emergency Food Reserve'],
        contactNumber: '01711000002',
        isActive: true
    },
    {
        id: 'sylhet_shelter_001',
        name: 'Gowainghat Elevated Flood Refuge',
        nameBn: 'গোয়াইনঘাট উঁচু বন্যা আশ্রয়কেন্দ্র',
        description: 'Reinforced 3-storey school shelter on embankment.',
        descriptionBn: 'বাঁধের ওপর ৩ তলা বিশিষ্ট পাকা আশ্রয়কেন্দ্র।',
        divisionId: 'sylhet',
        districtId: 'sylhet',
        upazilaId: 'gowainghat',
        address: 'Gowainghat Bazar, Sylhet',
        addressBn: 'গোয়াইনঘাট বাজার, সিলেট',
        latitude: 25.1050,
        longitude: 91.9750,
        capacity: 850,
        currentOccupancy: 120,
        shelterType: 'Flood Refuge',
        facilities: ['Water Purifier', 'First Aid', 'Sanitation', 'Solar Power'],
        contactNumber: '01711000003',
        isActive: true
    },
    {
        id: 'kurigram_shelter_001',
        name: 'Chilmari Char Community Shelter',
        nameBn: 'চিলমারী চর কমিউনিটি আশ্রয়কেন্দ্র',
        description: 'Plinth-raised flood shelter for river basin communities.',
        descriptionBn: 'নদী অববাহিকার জন্য মাটির ভিটি উঁচু করা বন্যা আশ্রয়কেন্দ্র।',
        divisionId: 'rangpur',
        districtId: 'kurigram',
        upazilaId: 'chilmari',
        address: 'Chilmari River Basin, Kurigram',
        addressBn: 'চিলমারী নদী তীর, কুড়িগ্রাম',
        latitude: 25.5500,
        longitude: 89.6700,
        capacity: 500,
        currentOccupancy: 45,
        shelterType: 'Flood Refuge',
        facilities: ['Drinking Water Well', 'Boat Dock', 'Dry Food Storage'],
        contactNumber: '01711000004',
        isActive: true
    },
    {
        id: 'satkhira_shelter_001',
        name: 'Shyamnagar Coastal Cyclone Fortress',
        nameBn: 'শ্যামনগর উপকূলীয় সাইক্লোন আশ্রয়কেন্দ্র',
        description: 'Tsunami and cyclone resistant shelter with livestock ramp.',
        descriptionBn: 'ঘূর্ণিঝড় ও জলোচ্ছ্বাস প্রতিরোধী আধুনিক আশ্রয়কেন্দ্র।',
        divisionId: 'khulna',
        districtId: 'satkhira',
        upazilaId: 'shyamnagar',
        address: 'Shyamnagar Embankment, Satkhira',
        addressBn: 'শ্যামনগর বাঁধ সংলগ্ন, সাতক্ষীরা',
        latitude: 22.3300,
        longitude: 89.1000,
        capacity: 900,
        currentOccupancy: 15,
        shelterType: 'Cyclone Shelter',
        facilities: ['Rainwater Harvesting', 'First Aid', 'Separate Sanitary Facilities'],
        contactNumber: '01711000005',
        isActive: true
    }
];

// -------------------------------------------------------------
// 5. DISASTER UPDATES
// -------------------------------------------------------------
const disasterUpdates = [
    {
        id: 'alert_cyclone_coastal_01',
        title: 'Cyclone Warning Signal No. 7 - Coastal Maritime Ports',
        titleBn: 'উপকূলীয় সমুদ্রবন্দরসমূহে ৭ নম্বর বিপদ সংকেত',
        description: 'Deep depression in Bay of Bengal intensified into severe cyclonic storm. Coastal districts advised to move vulnerable populations to designated shelters.',
        descriptionBn: 'বঙ্গোপসাগরে সৃষ্ট গভীর নিম্নচাপটি শক্তিশালী ঘূর্ণিঝড়ে রূপ নিয়েছে। উপকূলীয় ঝুঁকিপূর্ণ এলাকার বাসিন্দাদের অবিলম্বে আশ্রয়কেন্দ্রে যাওয়ার পরামর্শ দেওয়া হচ্ছে।',
        disasterType: 'Cyclone',
        severity: 'High',
        status: 'Active',
        divisionId: 'chattogram',
        districtId: 'coxs_bazar',
        upazilaId: 'teknaf',
        affectedAreas: ['Cox\'s Bazar', 'Chittagong', 'Noakhali', 'Bhola', 'Patuakhali'],
        issuedBy: 'Bangladesh Meteorological Department (BMD)',
        issuedAt: Date.now() - 3600000,
        expiresAt: Date.now() + 86400000,
        isActive: true
    },
    {
        id: 'alert_flood_sylhet_01',
        title: 'Flash Flood Warning - Surma and Kushiyara Rivers Flowing Above Danger Level',
        titleBn: 'আকস্মিক বন্যা সতর্কতা - সুরমা ও কুশিয়ারা নদীর পানি বিপৎসীমার উপর দিয়ে প্রবাহিত',
        description: 'Continuous upstream torrential rainfall has caused river basins in Sylhet and Sunamganj to overflow. Low-lying areas inundated.',
        descriptionBn: 'উজান থেকে নেমে আসা পাহাড়ি ঢল ও টানা ভারী বৃষ্টিপাতে সিলেট ও সুনামগঞ্জের নিম্নাঞ্চল প্লাবিত। জনসাধারণকে সতর্ক থাকার অনুরোধ।',
        disasterType: 'Flood',
        severity: 'Critical',
        status: 'Active',
        divisionId: 'sylhet',
        districtId: 'sylhet',
        upazilaId: 'gowainghat',
        affectedAreas: ['Sylhet Sadar', 'Gowainghat', 'Companiganj', 'Sunamganj'],
        issuedBy: 'Flood Forecasting and Warning Centre (FFWC / BWDB)',
        issuedAt: Date.now() - 7200000,
        expiresAt: Date.now() + 172800000,
        isActive: true
    },
    {
        id: 'alert_heatwave_khulna_01',
        title: 'Severe Heatwave Advisory Across Southwestern Districts',
        titleBn: 'দক্ষিণ-পশ্চিমাঞ্চলের জেলাসমূহে তীব্র তাপপ্রবাহের সতর্কবার্তা',
        description: 'Day temperatures expected to exceed 41°C. Stay hydrated, avoid direct sun between 11 AM and 4 PM, check on vulnerable family members.',
        descriptionBn: 'দিনের সর্বোচ্চ তাপমাত্রা ৪১ ডিগ্রি সেলসিয়াস অতিক্রম করতে পারে। প্রচুর পানি পান করুন এবং সরাসরি রোদ পরিহার করুন।',
        disasterType: 'Heatwave',
        severity: 'Moderate',
        status: 'Active',
        divisionId: 'khulna',
        districtId: 'khulna',
        upazilaId: 'dumuria',
        affectedAreas: ['Khulna', 'Jashore', 'Satkhira', 'Chuadanga'],
        issuedBy: 'Bangladesh Meteorological Department (BMD)',
        issuedAt: Date.now() - 14400000,
        expiresAt: Date.now() + 259200000,
        isActive: true
    }
];

// -------------------------------------------------------------
// MAIN EXECUTION
// -------------------------------------------------------------
async function runImport() {
    console.log('=== STARTING JIBONSHIELD BD FIRESTORE IMPORT ===');
    console.log(`Divisions count: ${divisions.length}`);
    console.log(`Districts count: ${districts.length}`);
    console.log(`Upazilas count:  ${upazilas.length}`);
    console.log(`Shelters count:  ${shelters.length}`);
    console.log(`Alerts count:    ${disasterUpdates.length}`);

    try {
        console.log('\n1. Importing Divisions...');
        await commitBatch('divisions', divisions);

        console.log('\n2. Importing Districts...');
        await commitBatch('districts', districts);

        console.log('\n3. Importing Upazilas...');
        await commitBatch('upazilas', upazilas);

        console.log('\n4. Importing Designated Shelters...');
        await commitBatch('shelters', shelters);

        console.log('\n5. Importing Disaster Updates...');
        await commitBatch('disaster_updates', disasterUpdates);

        console.log('\n=== FIRESTORE IMPORT COMPLETED SUCCESSFULLY! ===');
        process.exit(0);
    } catch (err) {
        console.error('Import failed with error:', err);
        process.exit(1);
    }
}

runImport();

