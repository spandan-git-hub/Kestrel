package com.kestrel.common.events;

import java.util.List;
import java.util.Optional;

/**
 * Production catalog of 45 real Indian river stations across major river basins:
 * West Coastal, Ganga Basin, Brahmaputra, Indus/North, Godavari, Krishna, Mahanadi,
 * Eastern, West-Flowing (Narmada, Tapi, Sabarmati), and Cauvery/South (Periyar, Pamba).
 */
public final class IndianRiverCatalog {

    public record RiverStation(
            String locationId,
            String name,
            String state,
            String riverName,
            String basin,
            double latitude,
            double longitude,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr,
            Language primaryLanguage,
            boolean active
    ) {}

    public static final List<RiverStation> ALL_45_STATIONS = List.of(
            // West Coastal
            new RiverStation("mumbai-kurla", "Kurla Ward L, Mumbai", "Maharashtra", "Mithi River", "West Coastal", 19.0726, 72.8797, 45.0, 85.0, 140.0, Language.mr, true),

            // Ganga Basin
            new RiverStation("patna-ward12", "Ward 12, Patna", "Bihar", "Ganges River", "Ganga Basin", 25.6127, 85.1589, 38000.0, 48000.0, 62000.0, Language.hi, true),
            new RiverStation("ganga-haridwar", "Haridwar Ghats", "Uttarakhand", "Ganges River", "Ganga Basin", 29.9457, 78.1642, 4200.0, 6500.0, 9800.0, Language.hi, true),
            new RiverStation("ganga-varanasi", "Varanasi Rajghat", "Uttar Pradesh", "Ganges River", "Ganga Basin", 25.3176, 83.0062, 28000.0, 37000.0, 49000.0, Language.hi, true),
            new RiverStation("ganga-bhagalpur", "Bhagalpur Sultanganj", "Bihar", "Ganges River", "Ganga Basin", 25.2425, 86.9842, 41000.0, 52000.0, 68000.0, Language.hi, true),
            new RiverStation("ganga-farakka", "Farakka Barrage", "West Bengal", "Ganges River", "Ganga Basin", 24.7981, 87.9175, 45000.0, 58000.0, 75000.0, Language.en, true),
            new RiverStation("yamuna-delhi", "Old Railway Bridge, Delhi", "Delhi", "Yamuna River", "Ganga Basin", 28.6600, 77.2400, 3200.0, 5100.0, 7800.0, Language.hi, true),
            new RiverStation("yamuna-agra", "Taj Corridor, Agra", "Uttar Pradesh", "Yamuna River", "Ganga Basin", 27.1751, 78.0421, 4500.0, 6800.0, 9500.0, Language.hi, true),
            new RiverStation("yamuna-prayagraj", "Sangam, Prayagraj", "Uttar Pradesh", "Yamuna River", "Ganga Basin", 25.4358, 81.8463, 12000.0, 18000.0, 26000.0, Language.hi, true),
            new RiverStation("gomti-lucknow", "Gomti Riverfront, Lucknow", "Uttar Pradesh", "Gomti River", "Ganga Basin", 26.8467, 80.9462, 650.0, 1100.0, 1800.0, Language.hi, true),
            new RiverStation("ghaghara-ayodhya", "Saryu Ghats, Ayodhya", "Uttar Pradesh", "Ghaghara River", "Ganga Basin", 26.7922, 82.1998, 14000.0, 21000.0, 31000.0, Language.hi, true),
            new RiverStation("gandak-hajipur", "Hajipur Gandak Bridge", "Bihar", "Gandak River", "Ganga Basin", 25.6858, 85.2144, 8500.0, 13000.0, 19000.0, Language.hi, true),
            new RiverStation("kosi-supaul", "Birpur / Supaul Embankment", "Bihar", "Kosi River", "Ganga Basin", 26.5245, 86.5982, 9200.0, 15500.0, 24000.0, Language.hi, true),
            new RiverStation("son-dehri", "Dehri Indrapuri Barrage", "Bihar", "Son River", "Ganga Basin", 24.9124, 84.1834, 7800.0, 12500.0, 18500.0, Language.hi, true),
            new RiverStation("hooghly-kolkata", "Howrah & Babu Ghat, Kolkata", "West Bengal", "Hooghly River", "Ganga Basin", 22.5697, 88.3426, 7200.0, 11500.0, 16800.0, Language.en, true),

            // Brahmaputra
            new RiverStation("guwahati-river", "Riverside, Guwahati", "Assam", "Brahmaputra River", "Brahmaputra", 26.1856, 91.7478, 32000.0, 42000.0, 55000.0, Language.en, true),
            new RiverStation("brahmaputra-dibrugarh", "Dibrugarh Reach", "Assam", "Brahmaputra River", "Brahmaputra", 27.4728, 94.9120, 24000.0, 33000.0, 44000.0, Language.en, true),
            new RiverStation("brahmaputra-dhubri", "Dhubri Port Reach", "Assam", "Brahmaputra River", "Brahmaputra", 26.0195, 89.9745, 38000.0, 49000.0, 64000.0, Language.en, true),
            new RiverStation("teesta-jalpaiguri", "Teesta Barrage, Jalpaiguri", "West Bengal", "Teesta River", "Brahmaputra", 26.5405, 88.7194, 4800.0, 7600.0, 11500.0, Language.en, true),
            new RiverStation("barak-silchar", "Annapurna Ghat, Silchar", "Assam", "Barak River", "Brahmaputra", 24.8333, 92.7789, 3400.0, 5200.0, 7800.0, Language.en, true),
            new RiverStation("subansiri-lakhimpur", "North Lakhimpur Reach", "Assam", "Subansiri River", "Brahmaputra", 27.2366, 94.1027, 5800.0, 8900.0, 13200.0, Language.en, true),

            // Indus / North
            new RiverStation("jhelum-srinagar", "Ram Munshi Bagh, Srinagar", "J&K", "Jhelum River", "Indus/North", 34.0837, 74.7973, 750.0, 1250.0, 1950.0, Language.en, true),
            new RiverStation("chenab-akhnoor", "Akhnoor Bridge, Jammu", "J&K", "Chenab River", "Indus/North", 32.8972, 74.7422, 4500.0, 7200.0, 11000.0, Language.en, true),
            new RiverStation("beas-mandi", "Victoria Bridge, Mandi", "Himachal Pradesh", "Beas River", "Indus/North", 31.7087, 76.9320, 1800.0, 3100.0, 5200.0, Language.hi, true),
            new RiverStation("sutlej-ropar", "Ropar Headworks", "Punjab", "Sutlej River", "Indus/North", 30.9664, 76.5270, 2800.0, 4600.0, 7400.0, Language.en, true),
            new RiverStation("ravi-madhopur", "Madhopur Headworks, Pathankot", "Punjab", "Ravi River", "Indus/North", 32.3688, 75.5973, 2200.0, 3800.0, 6100.0, Language.en, true),

            // Godavari
            new RiverStation("godavari-nashik", "Ramkund, Nashik", "Maharashtra", "Godavari River", "Godavari", 19.9975, 73.7898, 1600.0, 2900.0, 4800.0, Language.mr, true),
            new RiverStation("godavari-nanded", "Sachkhand Ghat, Nanded", "Maharashtra", "Godavari River", "Godavari", 19.1383, 77.3210, 6800.0, 11000.0, 16500.0, Language.mr, true),
            new RiverStation("godavari-bhadrachalam", "Bhadrachalam Bridge", "Telangana", "Godavari River", "Godavari", 17.6689, 80.8936, 22000.0, 31000.0, 44000.0, Language.en, true),
            new RiverStation("godavari-rajahmundry", "Arch Bridge, Rajahmundry", "Andhra Pradesh", "Godavari River", "Godavari", 16.9891, 81.7840, 28000.0, 39000.0, 56000.0, Language.en, true),

            // Krishna
            new RiverStation("krishna-sangli", "Irwin Bridge, Sangli", "Maharashtra", "Krishna River", "Krishna", 16.8524, 74.5815, 3200.0, 5400.0, 8500.0, Language.mr, true),
            new RiverStation("krishna-vijayawada", "Prakasam Barrage, Vijayawada", "Andhra Pradesh", "Krishna River", "Krishna", 16.5062, 80.6080, 19000.0, 27000.0, 38000.0, Language.en, true),
            new RiverStation("tungabhadra-kurnool", "Tungabhadra Confluence, Kurnool", "Andhra Pradesh", "Tungabhadra River", "Krishna", 15.8281, 78.0373, 7500.0, 11800.0, 17500.0, Language.en, true),
            new RiverStation("musi-hyderabad", "Purana Pul, Hyderabad", "Telangana", "Musi River", "Krishna", 17.3700, 78.4730, 550.0, 950.0, 1600.0, Language.en, true),

            // Mahanadi
            new RiverStation("cuttack-delta", "Cuttack Municipal Corp", "Odisha", "Mahanadi River", "Mahanadi", 20.4625, 85.8830, 18000.0, 26000.0, 35000.0, Language.en, true),
            new RiverStation("mahanadi-sambalpur", "Hirakud Downstream, Sambalpur", "Odisha", "Mahanadi River", "Mahanadi", 21.4669, 83.9812, 12000.0, 18500.0, 27000.0, Language.en, true),

            // Eastern
            new RiverStation("subarnarekha-jamshedpur", "Domuhani, Jamshedpur", "Jharkhand", "Subarnarekha River", "Eastern", 22.8046, 86.2029, 2400.0, 4100.0, 6800.0, Language.hi, true),

            // West-Flowing (Narmada, Tapi, Sabarmati)
            new RiverStation("narmada-hoshangabad", "Sethani Ghat, Narmadapuram", "Madhya Pradesh", "Narmada River", "West-Flowing", 22.7519, 77.7275, 9500.0, 14800.0, 22500.0, Language.hi, true),
            new RiverStation("narmada-bharuch", "Golden Bridge, Bharuch", "Gujarat", "Narmada River", "West-Flowing", 21.7051, 72.9959, 14000.0, 21500.0, 31000.0, Language.en, true),
            new RiverStation("tapi-surat", "Causeway & Hope Bridge, Surat", "Gujarat", "Tapi River", "West-Flowing", 21.1702, 72.8311, 5800.0, 9200.0, 14500.0, Language.en, true),
            new RiverStation("sabarmati-ahmedabad", "Riverfront, Ahmedabad", "Gujarat", "Sabarmati River", "West-Flowing", 23.0225, 72.5714, 1800.0, 3200.0, 5400.0, Language.en, true),

            // Cauvery / South
            new RiverStation("cauvery-srirangapatna", "Sangama, Srirangapatna", "Karnataka", "Cauvery River", "Cauvery/South", 12.4181, 76.6947, 2400.0, 4100.0, 6800.0, Language.en, true),
            new RiverStation("cauvery-trichy", "Grand Anicut, Tiruchirappalli", "Tamil Nadu", "Cauvery River", "Cauvery/South", 10.8271, 78.6890, 4800.0, 7900.0, 12200.0, Language.en, true),
            new RiverStation("periyar-aluva", "Manappuram Ghat, Aluva", "Kerala", "Periyar River", "Cauvery/South", 10.1076, 76.3516, 1900.0, 3300.0, 5200.0, Language.en, true),
            new RiverStation("pamba-chengangur", "Chengannur Bridge", "Kerala", "Pamba River", "Cauvery/South", 9.3175, 76.6122, 1400.0, 2500.0, 4100.0, Language.en, true)
    );

    private IndianRiverCatalog() {}

    public static List<RiverStation> getAllStations() {
        return ALL_45_STATIONS;
    }

    public static Optional<RiverStation> findById(String locationId) {
        return ALL_45_STATIONS.stream()
                .filter(s -> s.locationId().equalsIgnoreCase(locationId))
                .findFirst();
    }

    public static RegionType getRegionType(String locationId) {
        if (locationId == null) {
            return RegionType.PLAINS;
        }
        return switch (locationId.toLowerCase()) {
            case "mumbai-kurla", "hooghly-kolkata", "cuttack-delta",
                 "godavari-rajahmundry", "krishna-vijayawada", "narmada-bharuch",
                 "tapi-surat", "periyar-aluva", "pamba-chengangur" -> RegionType.COASTAL;
            case "jhelum-srinagar", "chenab-akhnoor", "beas-mandi",
                 "ganga-haridwar", "teesta-jalpaiguri" -> RegionType.HILLY;
            default -> RegionType.PLAINS;
        };
    }
}

