package com.ghost.browser

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

    private fun win(
        w: Int, h: Int, dpr: String, cores: Int, gpuV: String, gpuR: String
    ): Map<String, String> = mapOf(
        "family" to "windows",
        "ua" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$CHROME Safari/537.36",
        "model" to "",
        "androidVersion" to "15",
        "platform" to "Win32",
        "vendor" to "Google Inc.",
        "cores" to cores.toString(),
        "memory" to "8",
        "touch" to "0",
        "screenW" to w.toString(),
        "screenH" to h.toString(),
        "dpr" to dpr,
        "depth" to "24",
        "gpuVendor" to gpuV,
        "gpuRenderer" to gpuR
    )

    private fun mac(
        w: Int, h: Int, cores: Int
    ): Map<String, String> = mapOf(
        "family" to "mac",
        "ua" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.6 Safari/605.1.15",
        "model" to "",
        "androidVersion" to "15.6",
        "platform" to "MacIntel",
        "vendor" to "Apple Computer, Inc.",
        "cores" to cores.toString(),
        "memory" to "8",
        "touch" to "0",
        "screenW" to w.toString(),
        "screenH" to h.toString(),
        "dpr" to "2",
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

    val laptops: LinkedHashMap<String, Map<String, String>> = linkedMapOf(
        "Apple MacBook Pro 16" to mac(1728, 1117, 16),
        "Apple MacBook Air 15" to mac(1512, 982, 10),
        "Apple iMac 24" to mac(2048, 1152, 10),
        "Dell XPS 17" to win(1920, 1200, "1", 20, "Google Inc. (NVIDIA)", "ANGLE (NVIDIA, NVIDIA GeForce RTX 4070 Laptop GPU Direct3D11 vs_5_0 ps_5_0, D3D11)"),
        "Dell XPS 15" to win(1920, 1080, "1", 16, "Google Inc. (Intel)", "ANGLE (Intel, Intel(R) Arc(TM) Graphics Direct3D11 vs_5_0 ps_5_0, D3D11)"),
        "HP Spectre x360" to win(1920, 1280, "1", 12, "Google Inc. (Intel)", "ANGLE (Intel, Intel(R) Iris(R) Xe Graphics Direct3D11 vs_5_0 ps_5_0, D3D11)"),
        "Lenovo ThinkPad X1 Carbon" to win(1920, 1200, "1", 12, "Google Inc. (Intel)", "ANGLE (Intel, Intel(R) Iris(R) Xe Graphics Direct3D11 vs_5_0 ps_5_0, D3D11)"),
        "ASUS ROG Zephyrus G16" to win(2560, 1600, "1", 16, "Google Inc. (NVIDIA)", "ANGLE (NVIDIA, NVIDIA GeForce RTX 4080 Laptop GPU Direct3D11 vs_5_0 ps_5_0, D3D11)"),
        "Razer Blade 16" to win(2560, 1600, "1", 24, "Google Inc. (NVIDIA)", "ANGLE (NVIDIA, NVIDIA GeForce RTX 4090 Laptop GPU Direct3D11 vs_5_0 ps_5_0, D3D11)"),
        "Microsoft Surface Laptop Studio 2" to win(2400, 1600, "1", 20, "Google Inc. (NVIDIA)", "ANGLE (NVIDIA, NVIDIA GeForce RTX 4060 Laptop GPU Direct3D11 vs_5_0 ps_5_0, D3D11)")
    )

    val extra: LinkedHashMap<String, Map<String, String>> = linkedMapOf(
        "Samsung Galaxy S24 Ultra" to droid("SM-S928B", "15", 384, 824, "3.75", 8, 12, ADRENO, "Adreno (TM) 750"),
        "Samsung Galaxy S23" to droid("SM-S911B", "15", 360, 780, "3", 8, 8, ADRENO, "Adreno (TM) 740"),
        "Samsung Galaxy A55 5G" to droid("SM-A556B", "15", 360, 800, "3", 8, 8, ARM, "Mali-G68 MC4"),
        "Google Pixel 9 Pro" to droid("Pixel 9 Pro", "15", 412, 915, "2.625", 8, 16, ARM, "Mali-G715-Immortalis MC10"),
        "Google Pixel 8" to droid("Pixel 8", "15", 412, 915, "2.625", 8, 8, ARM, "Mali-G715-Immortalis MC10"),
        "Xiaomi 14" to droid("23127PN0CG", "15", 393, 873, "3.5", 8, 12, ADRENO, "Adreno (TM) 750"),
        "Redmi Note 13 Pro" to droid("23117RA68G", "14", 393, 873, "2.75", 8, 8, ADRENO, "Adreno (TM) 710"),
        "OnePlus 12" to droid("CPH2573", "15", 412, 919, "3.5", 8, 16, ADRENO, "Adreno (TM) 750"),
        "Realme GT 6" to droid("RMX3851", "14", 393, 873, "3", 8, 12, ADRENO, "Adreno (TM) 735"),
        "Nothing Phone (2a)" to droid("A142", "14", 412, 915, "2.6", 8, 8, ARM, "Mali-G610 MC4"),
        "Vivo V30" to droid("V2318", "14", 392, 872, "3", 8, 8, ADRENO, "Adreno (TM) 720"),
        "Tecno Camon 30" to droid("CL6", "14", 393, 873, "2.75", 8, 8, ARM, "Mali-G610 MC4"),
        "Infinix Note 40 Pro" to droid("X6850", "14", 393, 873, "2.75", 8, 8, ARM, "Mali-G610 MC4"),
        "Motorola Edge 50 Pro" to droid("XT2403-2", "14", 402, 896, "2.6", 8, 12, ADRENO, "Adreno (TM) 720"),
        "Apple iPhone 16 Pro Max" to ios("iPhone 16 Pro Max", "26.0", 440, 956, "3"),
        "Apple iPhone 16" to ios("iPhone 16", "26.0", 393, 852, "3"),
        "Apple iPhone 15 Pro" to ios("iPhone 15 Pro", "26.0", 393, 852, "3"),
        "Apple iPhone SE" to ios("iPhone SE", "26.0", 375, 667, "2"),
        "Samsung Galaxy Tab S10 Ultra" to droid("SM-X926B", "15", 800, 1280, "2", 8, 12, ARM, "Mali-G720-Immortalis MC12"),
        "Lenovo Legion Pro 7" to win(2560, 1600, "1", 24, "Google Inc. (NVIDIA)", "ANGLE (NVIDIA, NVIDIA GeForce RTX 4080 Laptop GPU Direct3D11 vs_5_0 ps_5_0, D3D11)")
    )

    class Country(
        val name: String, val tz: String, val lang: String, val langs: String,
        val lat: String, val lon: String
    )

    val countries: List<Country> = listOf(
        Country("Bangladesh", "Asia/Dhaka", "bn-BD", "bn-BD,bn,en", "23.8103", "90.4125"),
        Country("United States (New York)", "America/New_York", "en-US", "en-US,en", "40.7128", "-74.0060"),
        Country("United Kingdom", "Europe/London", "en-GB", "en-GB,en", "51.5072", "-0.1276"),
        Country("United Arab Emirates", "Asia/Dubai", "ar-AE", "ar-AE,ar,en", "25.2048", "55.2708"),
        Country("India", "Asia/Kolkata", "en-IN", "en-IN,hi,en", "28.6139", "77.2090"),
        Country("Japan", "Asia/Tokyo", "ja-JP", "ja-JP,ja,en", "35.6762", "139.6503"),
        Country("Germany", "Europe/Berlin", "de-DE", "de-DE,de,en", "52.5200", "13.4050"),
        Country("Australia", "Australia/Sydney", "en-AU", "en-AU,en", "-33.8688", "151.2093"),
        Country("Brazil", "America/Sao_Paulo", "pt-BR", "pt-BR,pt,en", "-23.5505", "-46.6333"),
        Country("Singapore", "Asia/Singapore", "en-SG", "en-SG,en", "1.3521", "103.8198")
    )
}
