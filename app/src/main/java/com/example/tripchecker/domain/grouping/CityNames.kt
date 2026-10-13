package com.example.tripchecker.domain.grouping

import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.Station
import com.example.tripchecker.domain.text.DisplayText

/** Human-readable city labels and route titles for journeys. */
object CityNames {
    /**
     * City label for [station]: the `StationClusters` city in title case, else the
     * title-cased station name, else the code. Null for a null station.
     */
    fun cityLabel(station: Station?): String? {
        if (station == null) return null
        val code = station.code.trim()
        if (StationClusters.isKnownCode(code)) {
            // City keys are plain words; some (PUNE) are also station codes, so skip the code rules.
            return StationClusters.cityOf(code).split(' ')
                .joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercaseChar() } }
        }
        val name = station.name
        if (!name.isNullOrBlank()) return DisplayText.titleCase(name, setOf(code))
        return code
    }

    /**
     * Route title for [group]: "A ⇄ B" for a round trip, "A → B" for one way and
     * "A → B → C" for custom chains (consecutive duplicates collapsed). Origin is
     * `boarding ?: from`.
     */
    fun routeTitle(group: JourneyGroup): String {
        val legs = group.legs
        if (legs.isEmpty()) return ""
        if (group.kind == JourneyKind.ROUND_TRIP) {
            val first = legs.first()
            val a = cityLabel(first.origin)
            val b = cityLabel(first.to)
            return listOfNotNull(a, b).joinToString(" ⇄ ")
        }
        val cities = ArrayList<String>()
        legs.forEach { leg ->
            listOf(cityLabel(leg.origin), cityLabel(leg.to)).forEach { c ->
                if (c != null && cities.lastOrNull() != c) cities.add(c)
            }
        }
        return cities.joinToString(" → ")
    }
}
