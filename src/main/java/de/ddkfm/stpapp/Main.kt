package de.ddkfm.stpapp

import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import com.mashape.unirest.http.Unirest
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.collections.HashMap

fun main(args : Array<String>) {
    val username = args[0]
    val password = args[1]
    val group = args[2]

    var globalCal = ICalendar()
    var mealCal = fetchMeal(globalCal)
    var stpappEvents = HashMap<Long, VEvent>()
    var minDay = fetchStpAppLectures(username, password, group, stpappEvents)
    var stpappCal = fetchCampusDualLectures(matriculationNumber = "5000923", hash = "e1434034bda56b611b7d244f87e2301f",
            minDay = minDay, stpappEvents = stpappEvents, globalCal = globalCal)

    File("./stpAndMeal.ical").writeText(Biweekly.write(globalCal).go(), Charset.forName("UTF-8"))
    File("./stp.ical").writeText(Biweekly.write(stpappCal).go(), Charset.forName("UTF-8"))
    File("./meal.ical").writeText(Biweekly.write(mealCal).go(), Charset.forName("UTF-8"))
}
fun getFullDate(date : String, time : String) : LocalDateTime {
    val year = date.split("/")[0].toInt()
    val month = date.split("/")[1].toInt()
    val day = date.split("/")[2].toInt()
    var hourDate = parseHourDate(time)
    var (hour, minute) = hourDate
    return LocalDateTime.of(year, month, day, hour, minute)
}
fun parseHourDate(hourDate : String) : HourDate {
    val hour = hourDate.toInt() / 60
    val minute = hourDate.toInt() - hour * 60
    return HourDate(hour, minute)
}

fun fetchCampusDualLectures(matriculationNumber: String, hash: String, minDay: Long, stpappEvents: Map<Long, VEvent>, globalCal: ICalendar) : ICalendar {
    var startTime = System.currentTimeMillis()
    var resp = Unirest.get("https://selfservice.campus-dual.de/room/json?userid=$matriculationNumber&hash=$hash") //start and end should be recognized in the response.....they should
            .asJson()
            .body
            .`array`
    println("time for fetching data from Campus-Dual: ${(System.currentTimeMillis() - startTime) / 1000}s")
    var cal = ICalendar()
    resp.forEach {
        var lecture = it as JSONObject
        var startDate = Date(lecture.getLong("start") * 1000)
        if(lecture.getLong("start") * 1000 >= minDay) {
            var endDate = Date(lecture.getLong("end") * 1000)
            //println(lecture.getString("description"))
            //println("${lecture.getLong("start")}: $startDate")
            //println("${lecture.getLong("end")}: $endDate")
            var event = VEvent()
            var stpappSummary = stpappEvents.get(startDate.time)?.summary?.value ?: lecture.getString("description")
            var summary = event.setSummary(stpappSummary)
            summary.language = "de-DE"
            event.setColor(lecture.getString("color"))
            event.setDateStart(startDate)
            event.setDateEnd(endDate)

            var room = lecture.getString("room")
            if (!room.isEmpty()) {
                room = room.replace("Seminarraum", "")
                room = room.replace("PC-Kabinett", "")

                if(stpappEvents.get(startDate.time) != null) {
                    var stpappRoom = stpappEvents.get(startDate.time)!!.location.value
                    stpappRoom = stpappRoom.replace("Raum", "").trim()
                    if(!room.trim().equals(stpappRoom)) {
                        println("collision detected: $room <> $stpappRoom ($startDate)")
                        room = "Raum $room(CampusDual) oder $stpappRoom (StpApp)"
                    }
                }
                event.setLocation(room)
            }
            var lectururer = lecture.getString("instructor")
            var description = "$stpappSummary\n"
            description += "Dozent: $lectururer\n"
            description += "Raum: $room\n"
            event.setDescription(description)
            println(description)
            cal.events.add(event)
            globalCal.events.add(event)
        }
    }
    return cal
}
fun fetchStpAppLectures(username : String, password : String, group : String, stpappEvents : MutableMap<Long, VEvent>) : Long {
    var resp = Unirest.get("http://stpapp.ba-leipzig.de/model/get/stundenplan.php?passwort=$password&benutzername=$username&seminargruppe=$group")
            .asJson()
            .body
            .`object`
    var data = resp.getJSONObject("data")
    var events = data.getJSONObject("termine")
    var lectures = data.getJSONObject("vorlesungen")
    var modules = data.getJSONObject("module")
    var lecturers = data.getJSONObject("dozenten")
    var defaultRoom = data.getString("defaultraum")
    var minDay = Date().time
    var eventCal = ICalendar()
    events.keySet().forEach {
        var day = it
        var eventString = "Day $day \n"
        var dayData = events.getJSONObject(day)
        dayData.keySet().forEach {
            var time = it

            var lecture = lectures.getJSONObject(dayData.getJSONObject(time).getString("vorlesung_id"))
            var lecturer = lecturers.getJSONObject(lecture.getJSONArray("dozent_id").getInt(0).toString())
            var jsonEvent = dayData.getJSONObject(time)
            var event = VEvent()
            var text = "Vorlesung: " + lecture.getString("name") + "\n"
            text += "Dozent: "
            if (!lecturer.isNull("titel"))
                text += lecturer.getString("titel") + " "
            if (!lecturer.isNull("vorname"))
                text += lecturer.getString("vorname") + " "
            if (!lecturer.isNull("name"))
                text += lecturer.getString("name")
            text += "\n"

            var room = if (jsonEvent.isNull("raum_id")) defaultRoom else {
                jsonEvent.getString("raum_id")
            }
            if (jsonEvent.isNull("raum_id"))
                text += "Raum $room \n"
            event.setLocation("Raum $room")

            var summary = event.setSummary(lecture.getString("name"))
            summary.language = "de-DE"
            event.setDescription(text)
            event.setColor(lecture.getString("farbe"))


            var start = getFullDate(day, time.split(".")[0])
            var end = getFullDate(day, time.split(".")[1])

            var timeInMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            minDay = Math.min(timeInMillis, minDay)

            event.setDateStart(Date(start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
            event.setDateEnd(Date(end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
            eventCal.addEvent(event)
            stpappEvents.put(timeInMillis, event)
        }
    }
    return minDay
}
fun fetchMeal(globalCal : ICalendar) : ICalendar {
    var mealCal = ICalendar()
    var mealResp = Unirest.get("http://stpapp.ba-leipzig.de/model/get/essen.php")
            .asJson()
            .body.
            `object`
    var data = mealResp.getJSONObject("data")
    data.keySet().forEach {
        var day = data.get(it)
        when(day) {
            is String ->  {}
            is JSONObject -> {
                var food1 = if(day.has("food1")) day.getJSONObject("food1") else null
                var food2 = if(day.has("food2")) day.getJSONObject("food2") else null

                if(food1 != null) {
                    var event1 = VEvent()
                    var summary = event1.setSummary(food1.getString("description"))
                    summary.language = "de-DE"
                    var date1 = SimpleDateFormat("dd.MM.yyyy HH:mm").parse("$it 11:30")

                    event1.setDateStart(date1)
                    event1.setDateEnd(Date(date1.time + 60 * 60 * 1000))
                    event1.setLocation(food1.getString("price").split(",")[0].trim())
                    event1.setDescription(food1.getString("description"))
                    globalCal.addEvent(event1)

                    mealCal.addEvent(event1)
                }
                if(food2 != null) {
                    var event2 = VEvent()
                    var summary = event2.setSummary(food2.getString("description"))
                    summary.language = "de-DE"
                    var date2 = SimpleDateFormat("dd.MM.yyyy HH:mm").parse("$it 13:30")

                    event2.setDateStart(date2)
                    event2.setDateEnd(Date(date2.time + 60 * 60 * 1000))
                    event2.setLocation(food2.getString("price").split(",")[0].trim())
                    event2.setDescription(food2.getString("description"))
                    globalCal.addEvent(event2)
                    mealCal.addEvent(event2)
                }
            }
        }
    }
    return mealCal
}