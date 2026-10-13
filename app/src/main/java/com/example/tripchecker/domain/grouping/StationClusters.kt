package com.example.tripchecker.domain.grouping

/** Maps IRCTC station codes to a city key for round-trip pairing. */
object StationClusters {
    private val codeToCity: Map<String, String> =
        buildMap {
            fun city(name: String, codes: List<String>) {
                codes.forEach { put(it.uppercase(), name) }
            }
            city("HYDERABAD", listOf("SC", "HYB", "KCG", "BMT", "LPI", "CHZ", "MJF", "HFZ"))
            city("MUMBAI", listOf("CSMT", "LTT", "BCT", "DR", "DDR", "BDTS", "TNA", "KYN", "PNVL", "BVI"))
            city("DELHI", listOf("NDLS", "DLI", "NZM", "ANVT", "DEE"))
            city("BENGALURU", listOf("SBC", "YPR", "SMVB", "BNC", "KJM"))
            city("CHENNAI", listOf("MAS", "MS", "TBM", "PER"))
            city("KOLKATA", listOf("HWH", "SDAH", "KOAA", "SRC", "SHM"))
            city("PUNE", listOf("PUNE", "HDP", "KK", "SVJR"))
            city("SHIRDI", listOf("SNSI", "KPG"))
        }

    /** True when [code] is one of the clustered station codes (case-insensitive). */
    fun isKnownCode(code: String?): Boolean = code != null && codeToCity.containsKey(code.uppercase())

    fun cityOf(code: String?): String {
        if (code.isNullOrBlank()) return code ?: ""
        return codeToCity[code.uppercase()] ?: code.uppercase()
    }
}
