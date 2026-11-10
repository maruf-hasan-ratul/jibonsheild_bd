const admin = require('firebase-admin');
const path = require('path');

const serviceAccount = require(path.join(__dirname, 'serviceAccountKey.json'));

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: 'jibonsheildbd'
});

const db = admin.firestore();
const FieldValue = admin.firestore.FieldValue;

const alerts = [
    {
        id: "alert_surma_001",
        title: "Surma-Kushiyara Severe Flash Flood",
        titleBn: "সুরমা-কুশিয়ারা তীব্র আকস্মিক বন্যা সতর্কবার্তা",
        description: "Surma River is flowing 68 cm above danger level at Kanaighat and 28 cm at Sylhet Sadar. Inundation spreading across low-lying unions.",
        descriptionBn: "মেঘালয় ও আসামে অতিভারী বর্ষণে সুরমা নদী কানাইঘাটে ৬৮ সেমি এবং সিলেট সদরে ২৮ সেমি বিপৎসীমার উপর দিয়ে প্রবাহিত হচ্ছে। নিম্নাঞ্চল প্লাবিত।",
        disasterType: "FLOOD",
        severity: "CRITICAL",
        status: "ACTIVE",
        divisionId: "sylhet",
        districtId: "sylhet",
        upazilaId: "sylhet_kanaighat",
        affectedAreas: ["Kanaighat", "Zakiganj", "Golapganj", "Sylhet Sadar", "Beanibazar"],
        issuedBy: "FFWC Bangladesh",
        issuedAt: new Date(),
        expiresAt: new Date(Date.now() + 86400000 * 4),
        isActive: true
    },
    {
        id: "alert_jamuna_002",
        title: "Brahmaputra-Jamuna Basin Major Flood Alert",
        titleBn: "ব্রহ্মপুত্র-যমুনা অববাহিকায় ভয়াবহ বন্যা সতর্কতা",
        description: "Jamuna water level rising at 18 cm/24h at Sirajganj and Sariakandi. Riverbank erosion and embankment overtopping imminent.",
        descriptionBn: "যমুনা নদীর পানি সিরাজগঞ্জ ও সারিয়াকান্দি পয়েন্টে দ্রুত বাড়ছে। নদী তীরবর্তী নিম্নাঞ্চল ও চরে বন্যা পরিস্থিতি অবনতিশীল।",
        disasterType: "FLOOD",
        severity: "CRITICAL",
        status: "ACTIVE",
        divisionId: "rajshahi",
        districtId: "sirajganj",
        upazilaId: "sirajganj_kazipur",
        affectedAreas: ["Kazipur", "Sirajganj Sadar", "Belkuchi", "Chauhali", "Shahjadpur"],
        issuedBy: "Flood Forecasting & Warning Centre",
        issuedAt: new Date(),
        expiresAt: new Date(Date.now() + 86400000 * 5),
        isActive: true
    },
    {
        id: "alert_kurigram_003",
        title: "Dharla & Dudhkumar Flood Inundation",
        titleBn: "ধরলা ও দুধকুমার নদীর পানি বৃদ্ধি ও প্লাবন",
        description: "Dharla river flowing 35 cm above danger level at Kurigram. Over 40,000 households across char areas stranded.",
        descriptionBn: "কুড়িগ্রাম পয়েন্টে ধরলা নদী বিপৎসীমার ৩৫ সেমি উপরে। রৌমারী, রাজিবপুর ও চিলমারী চরাঞ্চল প্লাবিত হয়েছে।",
        disasterType: "FLOOD",
        severity: "CRITICAL",
        status: "ACTIVE",
        divisionId: "rangpur",
        districtId: "kurigram",
        upazilaId: "kurigram_rajarhat",
        affectedAreas: ["Rajarhat", "Chilmari", "Ulipur", "Rowmari", "Nageshwari"],
        issuedBy: "BWDB & FFWC",
        issuedAt: new Date(),
        expiresAt: new Date(Date.now() + 86400000 * 3),
        isActive: true
    },
    {
        id: "alert_teesta_004",
        title: "Teesta River High Water Advisory",
        titleBn: "তিস্তা নদীর পানি বৃদ্ধি সতর্কবার্তা",
        description: "Teesta Barrage point at Dalia recorded discharge at maximum capacity. Gated flumes fully opened to release upstream run-off.",
        descriptionBn: "ডালিয়া পয়েন্টে তিস্তা নদীর পানি বিপৎসীমার কাছাকাছি। ডিমলা, হাতীবান্ধা ও পাটগ্রাম চরে সতর্কবার্তা জারি।",
        disasterType: "FLOOD",
        severity: "WARNING",
        status: "ACTIVE",
        divisionId: "rangpur",
        districtId: "lalmonirhat",
        upazilaId: "lalmonirhat_hatibandha",
        affectedAreas: ["Hatibandha", "Patgram", "Dimla", "Kaliganj"],
        issuedBy: "BWDB Dalia Division",
        issuedAt: new Date(),
        expiresAt: new Date(Date.now() + 86400000 * 3),
        isActive: true
    },
    {
        id: "alert_cyclone_005",
        title: "Bay of Bengal Coastal Cyclone & Surge Warning",
        titleBn: "বঙ্গোপসাগরে ঘূর্ণিঝড় ও জলোচ্ছ্বাস সতর্কবার্তা",
        description: "Deep tropical depression intensifying into cyclonic storm. Maritime signal 4 hoisted. 4-6 ft storm surge expected along coastlines.",
        descriptionBn: "বঙ্গোপসাগরে সৃষ্ট গভীর নিম্নচাপটি ঘূর্ণিঝড়ে রূপ নিচ্ছে। ৪ নম্বর স্থানীয় হুঁশিয়ারি সংকেত জারি। ৪-৬ ফুট উচ্চতার জলোচ্ছ্বাসের আশঙ্কা।",
        disasterType: "CYCLONE",
        severity: "WARNING",
        status: "ACTIVE",
        divisionId: "chittagong",
        districtId: "coxs_bazar",
        upazilaId: "coxs_bazar_chakaria",
        affectedAreas: ["Cox's Bazar Sadar", "Chakaria", "Moheshkhali", "Teknaf", "Kutubdia", "Sandwip"],
        issuedBy: "Bangladesh Meteorological Department (BMD)",
        issuedAt: new Date(),
        expiresAt: new Date(Date.now() + 86400000 * 2),
        isActive: true
    },
    {
        id: "alert_muhuri_006",
        title: "Muhuri River Embankment Breach Alert",
        titleBn: "মুহুরী নদীর বাঁধ ভাঙন ও প্লাবন সতর্কতা",
        description: "Heavy transboundary hill torrents caused multiple dyke breaches along Muhuri river in Parshuram and Fulgazi upazilas.",
        descriptionBn: "ভারতের ত্রিপুরা থেকে নেমে আসা পাহাড়ি ঢলে পরশুরাম ও ফুলগাজীতে মুহুরী নদীর বন্যা নিয়ন্ত্রণ বাঁধ ভেঙে বেশ কয়েকটি গ্রাম প্লাবিত।",
        disasterType: "FLOOD",
        severity: "CRITICAL",
        status: "ACTIVE",
        divisionId: "chittagong",
        districtId: "feni",
        upazilaId: "feni_parshuram",
        affectedAreas: ["Parshuram", "Fulgazi", "Chhagalnaiya"],
        issuedBy: "Feni District Administration & BWDB",
        issuedAt: new Date(),
        expiresAt: new Date(Date.now() + 86400000 * 4),
        isActive: true
    },
    {
        id: "alert_sundarbans_007",
        title: "Coastal Tidal Inundation Advisory",
        titleBn: "উপকূলীয় জোয়ারের পানি বৃদ্ধি ও বাঁধ সতর্কতা",
        description: "Spring tide combined with southerly wind forcing saline water through weak embankments across Dacope, Koyra, and Shyamnagar.",
        descriptionBn: "অমাবস্যার জোয়ার ও বাতাসের প্রভাবে দাকোপ, কয়রা ও শ্যামনগরের দুর্বল বেড়িবাঁধ উপচে লবণাক্ত পানি প্রবেশের আশঙ্কা।",
        disasterType: "FLOOD",
        severity: "ADVISORY",
        status: "ACTIVE",
        divisionId: "khulna",
        districtId: "khulna",
        upazilaId: "khulna_koyra",
        affectedAreas: ["Koyra", "Dacope", "Paikgachha", "Shyamnagar", "Assasuni"],
        issuedBy: "CPP & Local Disaster Management Committee",
        issuedAt: new Date(),
        expiresAt: new Date(Date.now() + 86400000 * 3),
        isActive: true
    },
    {
        id: "alert_dhaka_008",
        title: "Urban Torrential Rain & Waterlogging Advisory",
        titleBn: "ভারী বর্ষণজনিত জলাবদ্ধতা ও সতর্কতা",
        description: "Consecutive cloudbursts forecast over Greater Dhaka. Urban road waterlogging and slow drainage expected in low-lying canals.",
        descriptionBn: "ঢাকায় টানা ভারী বৃষ্টির পূর্বাভাস। মিরপুর, ডেমরা, ও টঙ্গী খাল এলাকায় জলাবদ্ধতার কারণে যান চলাচলে বিঘ্ন ঘটতে পারে।",
        disasterType: "FLOOD",
        severity: "ADVISORY",
        status: "ACTIVE",
        divisionId: "dhaka",
        districtId: "dhaka",
        upazilaId: "dhaka_mirpur",
        affectedAreas: ["Mirpur", "Demra", "Keraniganj", "Tongi"],
        issuedBy: "Dhaka North & South City Corporation Disaster Cells",
        issuedAt: new Date(),
        expiresAt: new Date(Date.now() + 86400000 * 2),
        isActive: true
    }
];

async function uploadAlerts() {
    console.log(`Starting upload of ${alerts.length} disaster updates to Firestore...`);
    const batch = db.batch();

    for (const alert of alerts) {
        const docRef = db.collection('disaster_updates').doc(alert.id);
        const data = { ...alert };
        delete data.id;
        data.updatedAt = FieldValue.serverTimestamp();
        data.issuedAt = alert.issuedAt;
        data.expiresAt = alert.expiresAt;
        batch.set(docRef, data, { merge: true });
    }

    await batch.commit();
    console.log(`Successfully uploaded ${alerts.length} disaster updates to Firestore collection 'disaster_updates'!`);
}

uploadAlerts().then(() => {
    process.exit(0);
}).catch(err => {
    console.error("Upload failed:", err);
    process.exit(1);
});

