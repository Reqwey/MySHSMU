package xyz.reqwey.myshsmu.model

data class ScoreItem(
    val courseName: String,
    val score: Double,
    val fScore: Double,
    val achievementGrade: String,
    val credit: Double,
    val examSituation: String,
    val semester: Int
)
