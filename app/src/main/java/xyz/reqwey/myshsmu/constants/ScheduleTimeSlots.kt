package xyz.reqwey.myshsmu.constants

import java.time.LocalTime

val STANDARD_TIME_SLOTS = listOf(
	LocalTime.of(8, 0) to "08:00\n08:40",
	LocalTime.of(8, 50) to "08:50\n09:30",
	LocalTime.of(9, 40) to "09:40\n10:20",
	LocalTime.of(10, 30) to "10:30\n11:10",
	LocalTime.of(11, 20) to "11:20\n12:00",
	LocalTime.of(12, 0) to "午间",
	LocalTime.of(13, 30) to "13:30\n14:10",
	LocalTime.of(14, 20) to "14:20\n15:00",
	LocalTime.of(15, 10) to "15:10\n15:50",
	LocalTime.of(16, 0) to "16:00\n16:40",
	LocalTime.of(16, 50) to "16:50\n17:30",
	LocalTime.of(17, 40) to "17:40\n18:20",
	LocalTime.of(18, 30) to "18:30\n19:10",
	LocalTime.of(19, 20) to "19:20\n20:00",
	LocalTime.of(20, 10) to "20:10\n20:50"
)