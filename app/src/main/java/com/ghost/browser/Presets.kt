package com.ghost.browser

/** ২০টি রেডিমেড ফোন ও ১০টি দেশের প্রিসেট। মডেল কোড ও স্ক্রিনের মান আনুমানিক — প্যানেলে এডিট করা যায়। */
object Presets {
    private const val CHROME = "141.0.0.0"

    private fun droid(
        model: String, ver: String, w: Int, h: Int, dpr: String,
        cores: Int, mem: Int, gpuV: String, gpuR: String
    ): Map<String, String> = mapOf(
        "family" to "android",
        "ua" to "Mozilla/5.0 (Linux; Android $ver; $model) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$CHROME Mobile Safari/537.36",
        "model" to model,
        "androidVersion" to ver,
        "platform" to "Linux armv8l",
        "vendor" to "Google Inc.",
        "cores" to cores.toString(),
        "memory" to mem.toString(),
        "touch" to "5",
        "screenW" to w.toString(),
        "screenH" to h.toString(),
        "dpr" to dpr,
        "depth" to "24",
        "gpuVendor" to gpuV,
        "gpuRenderer" to gpuR
    )

    private fun ios(
        model: String, ver: String, w: Int, h: Int, dpr: String
    ): Map<String, String> = mapOf(
        "family" to "ios",
        "ua" to "Mozilla/5.0 (iPhone; CPU iPhone OS 18_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/$ver Mobile/15E148 Safari/604.1",
        "model" to model,
        "androidVersion" to ver,
        "platform" to "iPhone",
        "vendor" to "Apple Computer, Inc.",
        "cores" to "6",
        "memory" to "8",
        "touch" to "5",
        "screenW" to w.toString(),
        "screenH" to h.toString(),
        "dpr" to dpr,
        "depth" to "24",
        "gpuVendor" to "Apple Inc.",
        "gpuRenderer" to "Apple GPU"
    )

    private val ADRENO = "Qualcomm"
    private val ARM = "ARM"

    val phones: LinkedHashMap<String, Map<String, String>> = linkedMapOf(
        "Apple iPhone 18 Pro Max" to ios("iPhone 18 Pro Max", "27.0", 440, 956, "3"),
        "Samsung Galaxy Z Fold 8 Ultra" to droid("SM-F978B", "17", 760, 960, "2.4", 8, 16, ADRENO, "Adreno (TM) 840"),
        "Samsung Galaxy S26 Ultra" to droid("SM-S948B", "17", 412, 915, "3.5", 8, 12, ADRENO, "Adreno (TM) 840"),
        "Apple iPhone 18 Pro" to ios("iPhone 18 Pro", "27.0", 402, 874, "3"),
        "Apple iPhone Duo" to ios("iPhone Duo", "27.0", 760, 1010, "3"),
        "Google Pixel 11 Pro" to droid("Pixel 11 Pro", "17", 412, 915, "2.625", 8, 16, ARM, "Mali-G720-Immortalis MC10"),
        "Vivo X300 Pro" to droid("V2501", "16", 412, 915, "3.5", 8, 16, ARM, "Mali-G925-Immortalis MC12"),
        "Honor Robot Phone" to droid("ROB-AN00", "16", 412, 915, "3", 8, 16, ADRENO, "Adreno (TM) 830"),
        "Xiaomi 17 Ultra" to droid("2509FPN0BC", "16", 440, 956, "3.25", 8, 16, ADRENO, "Adreno (TM) 840"),
        "OPPO Find X9 Pro" to droid("CPH2759", "16", 412, 915, "3.5", 8, 16, ARM, "Mali-G925-Immortalis MC12"),
        "OnePlus 15" to droid("CPH2749", "16", 412, 915, "3.5", 8, 16, ADRENO, "Adreno (TM) 840"),
        "Google Pixel 11 Pro XL" to droid("Pixel 11 Pro XL", "17", 448, 998, "2.625", 8, 16, ARM, "Mali-G720-Immortalis MC10"),
        "Motorola Razr Fold" to droid("XT2601-1", "16", 720, 900, "2.4", 8, 12, ADRENO, "Adreno (TM) 825"),
        "ASUS ROG Phone 9 Pro" to droid("ASUS_AI2501", "15", 393, 873, "2.75", 8, 16, ADRENO, "Adreno (TM) 830"),
        "iQOO 15" to droid("V2504A", "16", 412, 915, "3.5", 8, 16, ADRENO, "Adreno (TM) 840"),
        "Huawei Mate XT Ultimate Design" to droid("GRL-AL10", "12", 700, 900, "2.5", 8, 12, "Huawei", "Maleoon 910"),
        "Samsung Galaxy Z Flip 8" to droid("SM-F776B", "17", 412, 984, "2.625", 8, 12, ADRENO, "Adreno (TM) 830"),
        "Sony Xperia 1 VII" to droid("XQ-FS44", "16", 411, 960, "2.6", 8, 12, ADRENO, "Adreno (TM) 830"),
        "Vertu Metavertu 2" to droid("MetaVertu-2", "14", 412, 915, "2.6", 8, 12, ARM, "Mali-G715-Immortalis MC11"),
        "Vertu Ironflip" to droid("Ironflip", "14", 412, 915, "2.6", 8, 12, ARM, "Mali-G715-Immortalis MC11")
    )

    class Country(
        val name: String, val tz: String, val lang: String, val langs: String,
        val lat: String, val lon: String
    )

    val countries: List<Country> = listOf(
        Country("বাংলাদেশ", "Asia/Dhaka", "bn-BD", "bn-BD,bn,en", "23.8103", "90.4125"),
        Country("যুক্তরাষ্ট্র (নিউ ইয়র্ক)", "America/New_York", "en-US", "en-US,en", "40.7128", "-74.0060"),
        Country("যুক্তরাজ্য", "Europe/London", "en-GB", "en-GB,en", "51.5072", "-0.1276"),
        Country("সংযুক্ত আরব আমিরাত", "Asia/Dubai", "ar-AE", "ar-AE,ar,en", "25.2048", "55.2708"),
        Country("ভারত", "Asia/Kolkata", "en-IN", "en-IN,hi,en", "28.6139", "77.2090"),
        Country("জাপান", "Asia/Tokyo", "ja-JP", "ja-JP,ja,en", "35.6762", "139.6503"),
        Country("জার্মানি", "Europe/Berlin", "de-DE", "de-DE,de,en", "52.5200", "13.4050"),
        Country("অস্ট্রেলিয়া", "Australia/Sydney", "en-AU", "en-AU,en", "-33.8688", "151.2093"),
        Country("ব্রাজিল", "America/Sao_Paulo", "pt-BR", "pt-BR,pt,en", "-23.5505", "-46.6333"),
        Country("সিঙ্গাপুর", "Asia/Singapore", "en-SG", "en-SG,en", "1.3521", "103.8198")
    )
}
