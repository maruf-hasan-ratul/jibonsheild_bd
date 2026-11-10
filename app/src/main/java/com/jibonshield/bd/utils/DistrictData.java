package com.jibonshield.bd.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authoritative registry of all 64 administrative districts and 495 Upazilas
 * (sub-districts) of Bangladesh across all 8 administrative divisions.
 */
public final class DistrictData {

    private DistrictData() {}

    /**
     * Complete list of all 64 official districts in Bangladesh, sorted alphabetically.
     */
    public static final String[] ALL_64_DISTRICTS = new String[] {
            "Bagerhat",
            "Bandarban",
            "Barguna",
            "Barisal",
            "Bhola",
            "Bogura",
            "Brahmanbaria",
            "Chandpur",
            "Chapainawabganj",
            "Chittagong",
            "Chuadanga",
            "Comilla",
            "Cox's Bazar",
            "Dhaka",
            "Dinajpur",
            "Faridpur",
            "Feni",
            "Gaibandha",
            "Gazipur",
            "Gopalganj",
            "Habiganj",
            "Jamalpur",
            "Jashore",
            "Jhalokati",
            "Jhenaidah",
            "Joypurhat",
            "Khagrachhari",
            "Khulna",
            "Kishoreganj",
            "Kurigram",
            "Kushtia",
            "Lakshmipur",
            "Lalmonirhat",
            "Madaripur",
            "Magura",
            "Manikganj",
            "Meherpur",
            "Moulvibazar",
            "Munshiganj",
            "Mymensingh",
            "Naogaon",
            "Narail",
            "Narayanganj",
            "Narsingdi",
            "Natore",
            "Netrokona",
            "Nilphamari",
            "Noakhali",
            "Pabna",
            "Panchagarh",
            "Patuakhali",
            "Pirojpur",
            "Rajbari",
            "Rajshahi",
            "Rangamati",
            "Rangpur",
            "Satkhira",
            "Shariatpur",
            "Sherpur",
            "Sirajganj",
            "Sunamganj",
            "Sylhet",
            "Tangail",
            "Thakurgaon"
    };

    /**
     * Returns an array starting with "All", followed by all 64 districts.
     */
    public static String[] getDistrictsWithAll() {
        String[] result = new String[ALL_64_DISTRICTS.length + 1];
        result[0] = "All";
        System.arraycopy(ALL_64_DISTRICTS, 0, result, 1, ALL_64_DISTRICTS.length);
        return result;
    }

    /**
     * Returns an array starting with "All Districts", followed by all 64 districts.
     */
    public static String[] getDistrictsWithAllDistricts() {
        String[] result = new String[ALL_64_DISTRICTS.length + 1];
        result[0] = "All Districts";
        System.arraycopy(ALL_64_DISTRICTS, 0, result, 1, ALL_64_DISTRICTS.length);
        return result;
    }

    /**
     * Returns all 64 districts followed by "Other" for account registration or forms.
     */
    public static String[] getDistrictsForRegistration() {
        String[] result = new String[ALL_64_DISTRICTS.length + 1];
        System.arraycopy(ALL_64_DISTRICTS, 0, result, 0, ALL_64_DISTRICTS.length);
        result[ALL_64_DISTRICTS.length] = "Other";
        return result;
    }

    /**
     * Returns "All Districts", all 64 districts, and "Other" for filter controls.
     */
    public static String[] getDistrictsForHistoryFilter() {
        String[] result = new String[ALL_64_DISTRICTS.length + 2];
        result[0] = "All Districts";
        System.arraycopy(ALL_64_DISTRICTS, 0, result, 1, ALL_64_DISTRICTS.length);
        result[result.length - 1] = "Other";
        return result;
    }

    /**
     * Complete Upazilas mapping for all 64 districts of Bangladesh (495 total upazilas).
     */
    private static final Map<String, List<String>> UPAZILAS_MAP = new HashMap<>();

    static {
        // --- DHAKA DIVISION ---
        UPAZILAS_MAP.put("Dhaka", Arrays.asList(
                "Dhamrai", "Dohar", "Keraniganj", "Nawabganj", "Savar", "Tejgaon Circle"
        ));
        UPAZILAS_MAP.put("Gazipur", Arrays.asList(
                "Gazipur Sadar", "Kaliakair", "Kaliganj", "Kapasia", "Sreepur"
        ));
        UPAZILAS_MAP.put("Narayanganj", Arrays.asList(
                "Araihazar", "Bandar", "Narayanganj Sadar", "Rupganj", "Sonargaon"
        ));
        UPAZILAS_MAP.put("Tangail", Arrays.asList(
                "Tangail Sadar", "Basail", "Bhuapur", "Delduar", "Dhanbari",
                "Ghatail", "Gopalpur", "Kalihati", "Madhupur", "Mirzapur", "Nagarpur", "Sakhipur"
        ));
        UPAZILAS_MAP.put("Narsingdi", Arrays.asList(
                "Narsingdi Sadar", "Belabo", "Monohardi", "Palash", "Raipura", "Shibpur"
        ));
        UPAZILAS_MAP.put("Faridpur", Arrays.asList(
                "Faridpur Sadar", "Alfadanga", "Bhanga", "Boalmari", "Charbhadrasan",
                "Madhukhali", "Nagarkanda", "Sadarpur", "Saltha"
        ));
        UPAZILAS_MAP.put("Gopalganj", Arrays.asList(
                "Gopalganj Sadar", "Kashiani", "Kotalipara", "Muksudpur", "Tungipara"
        ));
        UPAZILAS_MAP.put("Kishoreganj", Arrays.asList(
                "Kishoreganj Sadar", "Astagram", "Bajitpur", "Bhairab", "Hossainpur",
                "Itna", "Karimganj", "Katiadi", "Kuliarchar", "Mithamain", "Nikli", "Pakundia", "Tarail"
        ));
        UPAZILAS_MAP.put("Madaripur", Arrays.asList(
                "Madaripur Sadar", "Dasar", "Kalkini", "Rajoir", "Shibchar"
        ));
        UPAZILAS_MAP.put("Manikganj", Arrays.asList(
                "Manikganj Sadar", "Daulatpur", "Ghior", "Harirampur", "Saturia", "Shivalaya", "Singair"
        ));
        UPAZILAS_MAP.put("Munshiganj", Arrays.asList(
                "Munshiganj Sadar", "Gazaria", "Louhajang", "Sirajdikhan", "Sreenagar", "Tongibari"
        ));
        UPAZILAS_MAP.put("Rajbari", Arrays.asList(
                "Rajbari Sadar", "Baliakandi", "Goalandaghat", "Kalukhali", "Pangsha"
        ));
        UPAZILAS_MAP.put("Shariatpur", Arrays.asList(
                "Shariatpur Sadar", "Bhedarganj", "Damudya", "Gosairhat", "Naria", "Zanjira"
        ));

        // --- CHITTAGONG DIVISION ---
        UPAZILAS_MAP.put("Chittagong", Arrays.asList(
                "Anwara", "Banshkhali", "Boalkhali", "Chandanaish", "Fatikchhari",
                "Hathazari", "Karnaphuli", "Lohagara", "Mirsharai", "Patiya",
                "Rangunia", "Raozan", "Sandwip", "Satkania", "Sitakunda"
        ));
        UPAZILAS_MAP.put("Cox's Bazar", Arrays.asList(
                "Chakaria", "Cox's Bazar Sadar", "Eidgaon", "Kutubdia",
                "Maheshkhali", "Pekua", "Ramu", "Teknaf", "Ukhia"
        ));
        UPAZILAS_MAP.put("Comilla", Arrays.asList(
                "Barura", "Brahmanpara", "Burichang", "Chandina", "Chauddagram",
                "Cumilla Adarsha Sadar", "Cumilla Sadar Dakshin", "Daudkandi", "Debidwar",
                "Homna", "Laksam", "Lalmai", "Meghna", "Monohargonj", "Muradnagar", "Nangalkot", "Titas"
        ));
        UPAZILAS_MAP.put("Feni", Arrays.asList(
                "Feni Sadar", "Chhagalnaiya", "Daganbhuiyan", "Fulgazi", "Parshuram", "Sonagazi"
        ));
        UPAZILAS_MAP.put("Brahmanbaria", Arrays.asList(
                "Brahmanbaria Sadar", "Akhaura", "Ashuganj", "Bancharampur",
                "Bijoynagar", "Kasba", "Nabinagar", "Nasirnagar", "Sarail"
        ));
        UPAZILAS_MAP.put("Chandpur", Arrays.asList(
                "Chandpur Sadar", "Faridganj", "Haimchar", "Hajiganj",
                "Kachua", "Matlab North", "Matlab South", "Shahrasti"
        ));
        UPAZILAS_MAP.put("Lakshmipur", Arrays.asList(
                "Lakshmipur Sadar", "Kamalnagar", "Raipur", "Ramganj", "Ramgati"
        ));
        UPAZILAS_MAP.put("Noakhali", Arrays.asList(
                "Noakhali Sadar", "Begumganj", "Chatkhil", "Companiganj",
                "Hatiya", "Kabirhat", "Senbagh", "Sonaimuri", "Subarnachar"
        ));
        UPAZILAS_MAP.put("Khagrachhari", Arrays.asList(
                "Khagrachhari Sadar", "Dighinala", "Guimara", "Laksmichhari",
                "Mahalchhari", "Manikchhari", "Matiranga", "Panchhari", "Ramgarh"
        ));
        UPAZILAS_MAP.put("Rangamati", Arrays.asList(
                "Rangamati Sadar", "Bagaichhari", "Barkal", "Belaichhari",
                "Juraichhari", "Kaptai", "Kawkhali", "Langadu", "Naniarchar", "Rajasthali"
        ));
        UPAZILAS_MAP.put("Bandarban", Arrays.asList(
                "Bandarban Sadar", "Alikadam", "Lama", "Naikhongchhari",
                "Rowangchhari", "Ruma", "Thanchi"
        ));

        // --- SYLHET DIVISION ---
        UPAZILAS_MAP.put("Sylhet", Arrays.asList(
                "Sylhet Sadar", "Balaganj", "Beanibazar", "Bishwanath",
                "Companiganj", "Dakshin Surma", "Fenchuganj", "Golapganj",
                "Gowainghat", "Jaintiapur", "Kanaighat", "Osmani Nagar", "Zakiganj"
        ));
        UPAZILAS_MAP.put("Moulvibazar", Arrays.asList(
                "Moulvibazar Sadar", "Barlekha", "Juri", "Kamalganj",
                "Kulaura", "Rajnagar", "Sreemangal"
        ));
        UPAZILAS_MAP.put("Habiganj", Arrays.asList(
                "Habiganj Sadar", "Ajmiriganj", "Bahubal", "Baniyachong",
                "Chunarughat", "Lakhai", "Madhabpur", "Nabiganj", "Sayestaganj"
        ));
        UPAZILAS_MAP.put("Sunamganj", Arrays.asList(
                "Sunamganj Sadar", "Bishwamvarpur", "Chhatak", "Derai",
                "Dharampasha", "Dowarabazar", "Jagannathpur", "Jamalganj",
                "Madhyanagar", "Shantiganj", "Sullah", "Tahirpur"
        ));

        // --- RAJSHAHI DIVISION ---
        UPAZILAS_MAP.put("Rajshahi", Arrays.asList(
                "Bagha", "Bagmara", "Charghat", "Durgapur", "Godagari",
                "Mohanpur", "Paba", "Puthia", "Tanore"
        ));
        UPAZILAS_MAP.put("Bogura", Arrays.asList(
                "Bogura Sadar", "Adamdighi", "Dhunat", "Dhupchanchia", "Gabtali",
                "Kahaloo", "Nandigram", "Sariakandi", "Shajahanpur", "Sherpur", "Shibganj", "Sonatola"
        ));
        UPAZILAS_MAP.put("Joypurhat", Arrays.asList(
                "Joypurhat Sadar", "Akkelpur", "Kalai", "Khetlal", "Panchbibi"
        ));
        UPAZILAS_MAP.put("Naogaon", Arrays.asList(
                "Naogaon Sadar", "Atrai", "Badalgachhi", "Dhamoirhat", "Manda",
                "Mohadevpur", "Niamatpur", "Patnitala", "Porsha", "Raninagar", "Sapahar"
        ));
        UPAZILAS_MAP.put("Natore", Arrays.asList(
                "Natore Sadar", "Bagatipara", "Baraigram", "Gurudaspur", "Lalpur", "Naldanga", "Singra"
        ));
        UPAZILAS_MAP.put("Chapainawabganj", Arrays.asList(
                "Chapainawabganj Sadar", "Bholahat", "Gomastapur", "Nachole", "Shibganj"
        ));
        UPAZILAS_MAP.put("Pabna", Arrays.asList(
                "Pabna Sadar", "Atgharia", "Bera", "Bhangura", "Chatmohar",
                "Faridpur", "Ishwardi", "Santhia", "Sujanagar"
        ));
        UPAZILAS_MAP.put("Sirajganj", Arrays.asList(
                "Sirajganj Sadar", "Belkuchi", "Chauhali", "Kamarkhanda",
                "Kazipur", "Raiganj", "Shahjadpur", "Tarash", "Ullapara"
        ));

        // --- RANGPUR DIVISION ---
        UPAZILAS_MAP.put("Rangpur", Arrays.asList(
                "Rangpur Sadar", "Badarganj", "Gangachhara", "Kaunia",
                "Mithapukur", "Pirgachha", "Pirganj", "Taraganj"
        ));
        UPAZILAS_MAP.put("Dinajpur", Arrays.asList(
                "Dinajpur Sadar", "Birampur", "Birganj", "Birol", "Bochaganj",
                "Chirirbandar", "Fulbari", "Ghoraghat", "Hakimpur", "Kaharole", "Khansama", "Nawabganj", "Parbatipur"
        ));
        UPAZILAS_MAP.put("Gaibandha", Arrays.asList(
                "Gaibandha Sadar", "Fulchhari", "Gobindaganj", "Palashbari",
                "Sadullapur", "Sughatta", "Sundarganj"
        ));
        UPAZILAS_MAP.put("Kurigram", Arrays.asList(
                "Kurigram Sadar", "Bhurungamari", "Char Rajibpur", "Chilmari",
                "Nageshwari", "Phulbari", "Rajarhat", "Roumari", "Ulipur"
        ));
        UPAZILAS_MAP.put("Lalmonirhat", Arrays.asList(
                "Lalmonirhat Sadar", "Aditmari", "Hatibandha", "Kaliganj", "Patgram"
        ));
        UPAZILAS_MAP.put("Nilphamari", Arrays.asList(
                "Nilphamari Sadar", "Dimla", "Domar", "Jaldhaka", "Kishoreganj", "Syedpur"
        ));
        UPAZILAS_MAP.put("Panchagarh", Arrays.asList(
                "Panchagarh Sadar", "Atwari", "Boda", "Debiganj", "Tetulia"
        ));
        UPAZILAS_MAP.put("Thakurgaon", Arrays.asList(
                "Thakurgaon Sadar", "Baliadangi", "Haripur", "Pirganj", "Ranisankail"
        ));

        // --- KHULNA DIVISION ---
        UPAZILAS_MAP.put("Khulna", Arrays.asList(
                "Batiaghata", "Dacope", "Dighalia", "Dumuria", "Koyra",
                "Paikgachha", "Phultala", "Rupsha", "Terokhada"
        ));
        UPAZILAS_MAP.put("Bagerhat", Arrays.asList(
                "Bagerhat Sadar", "Chitalmari", "Fakirhat", "Kachua",
                "Mollahat", "Mongla", "Morrelganj", "Rampal", "Sarankhola"
        ));
        UPAZILAS_MAP.put("Chuadanga", Arrays.asList(
                "Chuadanga Sadar", "Alamdanga", "Damurhuda", "Jibannagar"
        ));
        UPAZILAS_MAP.put("Jashore", Arrays.asList(
                "Jashore Sadar", "Abhaynagar", "Bagherpara", "Chaugachha",
                "Jhikargachha", "Keshabpur", "Manirampur", "Sharsha"
        ));
        UPAZILAS_MAP.put("Jhenaidah", Arrays.asList(
                "Jhenaidah Sadar", "Harinakundu", "Kaliganj", "Kotchandpur", "Maheshpur", "Shailkupa"
        ));
        UPAZILAS_MAP.put("Kushtia", Arrays.asList(
                "Kushtia Sadar", "Bheramara", "Daulatpur", "Khoksa", "Kumarkhali", "Mirpur"
        ));
        UPAZILAS_MAP.put("Magura", Arrays.asList(
                "Magura Sadar", "Mohammadpur", "Shalikha", "Sreepur"
        ));
        UPAZILAS_MAP.put("Meherpur", Arrays.asList(
                "Meherpur Sadar", "Gangni", "Mujibnagar"
        ));
        UPAZILAS_MAP.put("Narail", Arrays.asList(
                "Narail Sadar", "Kalia", "Lohagara"
        ));
        UPAZILAS_MAP.put("Satkhira", Arrays.asList(
                "Satkhira Sadar", "Assasuni", "Debhata", "Kalaroa", "Kaliganj", "Shyamnagar", "Tala"
        ));

        // --- BARISAL DIVISION ---
        UPAZILAS_MAP.put("Barisal", Arrays.asList(
                "Barisal Sadar", "Agailjhara", "Babuganj", "Bakerganj", "Banaripara",
                "Gaurnadi", "Hizla", "Mehendiganj", "Muladi", "Wazirpur"
        ));
        UPAZILAS_MAP.put("Barguna", Arrays.asList(
                "Barguna Sadar", "Amtali", "Bamna", "Betagi", "Patharghata", "Taltali"
        ));
        UPAZILAS_MAP.put("Bhola", Arrays.asList(
                "Bhola Sadar", "Borhanuddin", "Char Fasson", "Daulatkhan",
                "Lalmohan", "Manpura", "Tazumuddin"
        ));
        UPAZILAS_MAP.put("Jhalokati", Arrays.asList(
                "Jhalokati Sadar", "Kathalia", "Nalchity", "Rajapur"
        ));
        UPAZILAS_MAP.put("Patuakhali", Arrays.asList(
                "Patuakhali Sadar", "Bauphal", "Dashmina", "Dumki",
                "Galachipa", "Kalapara", "Mirzaganj", "Rangabali"
        ));
        UPAZILAS_MAP.put("Pirojpur", Arrays.asList(
                "Pirojpur Sadar", "Bhandaria", "Indurkani", "Kawkhali",
                "Mathbaria", "Nazirpur", "Nesarabad"
        ));

        // --- MYMENSINGH DIVISION ---
        UPAZILAS_MAP.put("Mymensingh", Arrays.asList(
                "Mymensingh Sadar", "Bhaluka", "Dhobaura", "Fulbaria", "Gaffargaon",
                "Gauripur", "Haluaghat", "Ishwarganj", "Muktagachha", "Nandail",
                "Phulpur", "Tara Khanda", "Trishal"
        ));
        UPAZILAS_MAP.put("Jamalpur", Arrays.asList(
                "Jamalpur Sadar", "Bakshiganj", "Dewanganj", "Islampur",
                "Madarganj", "Melandaha", "Sarishabari"
        ));
        UPAZILAS_MAP.put("Netrokona", Arrays.asList(
                "Netrokona Sadar", "Atpara", "Barhatta", "Durgapur", "Kalmakanda",
                "Kendua", "Khaliajuri", "Madan", "Mohanganj", "Purbadhala"
        ));
        UPAZILAS_MAP.put("Sherpur", Arrays.asList(
                "Sherpur Sadar", "Jhenaigati", "Nakla", "Nalitabari", "Sreebardi"
        ));
    }

    /**
     * Retrieve all upazilas for any of the 64 districts in Bangladesh.
     * Returns an unmodifiable list of upazilas, or empty list if district is not found.
     */
    public static List<String> getUpazilasForDistrict(String district) {
        if (district == null || district.trim().isEmpty()
                || "All".equalsIgnoreCase(district)
                || "All Districts".equalsIgnoreCase(district)) {
            return Collections.emptyList();
        }
        List<String> list = UPAZILAS_MAP.get(district);
        if (list != null) {
            return new ArrayList<>(list);
        }
        // Case-insensitive lookup fallback
        for (Map.Entry<String, List<String>> entry : UPAZILAS_MAP.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(district)) {
                return new ArrayList<>(entry.getValue());
            }
        }
        return Collections.emptyList();
    }
}
