package com.coachassist.services

import com.coachassist.models.Activity
import com.coachassist.models.SessionRequest
import com.coachassist.models.SessionResponse
import kotlin.math.roundToInt

class SessionPlannerService {

    // Private data class to represent a drill with its tailoring logic
    private data class Drill(
        val name: String,
        val description: String,
        // A function that takes (player count, base description) and returns a tailored description
        val tailor: (playerCount: Int, baseDescription: String) -> String = { _, base -> base }
    )

    // Centralized, data-driven definition of all drills. This can easily be moved to a database later.
    private val drillsByFocus = mapOf(
        "passing" to listOf(
            Drill(
                name = "Warm-up: Passing in Pairs",
                description = "Players pair up and pass the ball to each other over increasing distances.",
                tailor = { playerCount, baseDesc ->
                    val pairsDesc = if (playerCount % 2 == 0) "With $playerCount players, form ${playerCount / 2} pairs." else "With $playerCount players, form ${(playerCount - 1) / 2} pairs. The extra player can work with the coach."
                    "$pairsDesc $baseDesc"
                }
            ),
            Drill(
                name = "Main Drill: Piggy in the Middle",
                description = "A group of players forms a circle and tries to keep the ball away from one or two defenders in the middle.",
                tailor = { playerCount, baseDesc ->
                    if (playerCount >= 5) "With $playerCount players, create a group of ${playerCount - 2} attackers and 2 defenders. If numbers are lower, use one defender." else baseDesc
                }
            ),
            Drill(
                name = "Game: Small-sided Match",
                description = "A 4v4 or 5v5 match with a focus on quick, accurate passing to create scoring opportunities.",
                tailor = { playerCount, _ ->
                    if (playerCount >= 8) "A ${playerCount / 2}v${playerCount / 2} match with a focus on quick, accurate passing." else "A small-sided match. If numbers are low, use one goal and play 'king of the court'."
                }
            )
        ),
        "shooting" to listOf(
            Drill(
                name = "Warm-up: Dribble and Shoot",
                description = "Players dribble towards the goal from various angles and take a shot.",
                tailor = { playerCount, baseDesc -> if (playerCount > 8) "With $playerCount players, form two lines to keep the drill moving quickly. $baseDesc" else baseDesc }
            ),
            Drill(
                name = "Main Drill: Rebound Challenge",
                description = "Players take shots and follow up to score from any rebounds off the goalkeeper or posts.",
                tailor = { playerCount, baseDesc -> "Set up a rotation system to ensure all $playerCount players get plenty of shots. $baseDesc" }
            ),
            Drill(
                name = "Game: Shooting Competition",
                description = "Divide into two teams. Players take turns shooting from a designated spot. The team with the most goals wins.",
                tailor = { playerCount, baseDesc -> if (playerCount >= 4) "Divide the $playerCount players into two teams of ${playerCount / 2}. $baseDesc" else baseDesc }
            )
        ),
        "defence" to listOf(
            Drill("Warm-up: 1v1 Defending", "An attacker tries to dribble past a defender to a goal line. The defender's job is to stop them."),
            Drill("Main Drill: Defensive Shape", "Coach directs the ball around, and the defensive unit (e.g., back four) must shift and maintain their shape."),
            Drill("Game: Defend the Goal", "A small-sided game where one team has more players and attacks, while the other team focuses solely on defending their goal.")
        )
    )

    fun generatePlan(request: SessionRequest): SessionResponse {
        val percentages = parseAndValidateBreakdown(request.timeBreakdown)
        val drills = drillsByFocus[request.focus.lowercase()]
            ?: throw IllegalArgumentException("Unknown focus area: '${request.focus}'. Supported areas are: ${drillsByFocus.keys.joinToString()}")

        val activities = getActivitiesForFocus(drills, request.numberOfPlayers)

        if (percentages.size != activities.size) {
            throw IllegalArgumentException("Time breakdown parts (${percentages.size}) do not match the number of activities for the focus '${request.focus}' (${activities.size}). A session for this focus has ${activities.size} parts.")
        }

        val plannedActivities = activities.zip(percentages).map { (activity, percentage) ->
            val activityDuration = (request.durationMinutes * (percentage / 100.0)).roundToInt()
            activity.copy(durationMinutes = activityDuration)
        }

        return SessionResponse(
            focus = request.focus,
            totalDurationMinutes = request.durationMinutes,
            activities = plannedActivities
        )
    }

    private fun parseAndValidateBreakdown(breakdown: String): List<Int> {
        val parts = breakdown.split('/')
        val percentages = try {
            parts.map { it.toInt() }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid time breakdown format. Please use numbers separated by '/'.")
        }

        if (percentages.any { it <= 0 }) {
            throw IllegalArgumentException("Time breakdown percentages must be positive numbers.")
        }

        if (percentages.sum() != 100) {
            throw IllegalArgumentException("Time breakdown percentages must sum to 100.")
        }

        return percentages
    }

    private fun getActivitiesForFocus(drills: List<Drill>, numberOfPlayers: Int?): List<Activity> {
        return drills.map { drill ->
            val tailoredDescription = numberOfPlayers?.let { drill.tailor(it, drill.description) } ?: drill.description
            Activity(
                name = drill.name,
                description = tailoredDescription,
                durationMinutes = 0 // This will be calculated later
            )
        }
    }
}