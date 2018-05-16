package de.ddkfm.stpapp

import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import com.mashape.unirest.http.Unirest
import com.xenomachina.argparser.ArgParser
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.collections.ArrayList

var campusDualTime : Long = 0
var stpappTime : Double = 0.0
fun main(args : Array<String>) {
    ArgParser(args).parseInto(::ParamParser).run {
        println("#### Version 1.7 ####")
        println(args.joinToString())
        var globalCal = ICalendar()
        var mealCal = fetchMeal(globalCal)
        var stpappEvents = TreeMap<Long, VEvent>()
        var minDay = fetchStpAppLectures(stpappUsername, stpappPasswd, stpappGroup, stpappEvents)
        var stpappCal = fetchCampusDualLectures(matriculationNumber = campusDualMatriculationNumber, hash = campusDualHash,
                minDay = minDay, stpappEvents = stpappEvents, globalCal = globalCal)

        File("$icalOutputPath/stpAndMeal.ical").writeText(Biweekly.write(globalCal).go(), Charset.forName("UTF-8"))
        File("$icalOutputPath/stp.ical").writeText(Biweekly.write(stpappCal).go(), Charset.forName("UTF-8"))
        File("$icalOutputPath/meal.ical").writeText(Biweekly.write(mealCal).go(), Charset.forName("UTF-8"))

        File("$times").appendText("${Date().time}|$campusDualTime|$stpappTime\n")

        generateChart(times, chartOutputPath)
    }
}

data class Point(var x : Long, var y : Int)
data class Point2(var x : Long, var y : Double)

fun generateChart(times: String, chartOutputPath: String) {
    var content = File("./chart_template.html").readText(charset = Charset.forName("UTF-8"))
    var lines = File("$times").readLines(charset = Charset.forName("UTF-8"))
    var times = ArrayList<Point>()
    var timesStapp = ArrayList<Point2>()
    for(line in lines) {
        if(line.contains(":")) { //old method
            var x = line.split(":")[0].trim().toLong()
            var y = line.split(":")[1].trim().replace("s", "").toInt()
            times.add(Point(x, y))
        } else {
            var x = line.split("|")[0].trim().toLong()
            var y = line.split("|")[1].trim().toInt()
            times.add(Point(x, y))
            var y2 = line.split("|")[2].trim().toDouble()
            timesStapp.add(Point2(x, y2))
        }

    }
    var timesAsString = times
            .map { "{x : new Date(${it.x}), y : ${it.y}}" }
            .joinToString(separator = ",", prefix = "[", postfix = "]")
    var timesStpAppAsString = timesStapp
            .map { "{x : new Date(${it.x}), y : ${it.y}}" }
            .joinToString(separator = ",", prefix = "[", postfix = "]")
    content = content.replace("##TIMES##", timesAsString)
    content = content.replace("##TIMESSTPAPP##", timesStpAppAsString)
    File("$chartOutputPath/chart.html").writeText(content, charset = Charset.forName("UTF-8"))
}
fun getFullDate(date : String, time : String) : Date {
    val year = date.split("/")[0].toInt()
    val month = date.split("/")[1].toInt()
    val day = date.split("/")[2].toInt()
    var hourDate = parseHourDate(time)
    var (hour, minute) = hourDate
    var localDateTime = LocalDateTime.of(year, month, day, hour, minute)
    return Date.from(localDateTime.atZone(ZoneId.of("Europe/Berlin")).toInstant())
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
    campusDualTime = (System.currentTimeMillis() - startTime) / 1000
    //File("./times.txt").appendText("${Date().time}: ${(System.currentTimeMillis() - startTime) / 1000}s\n")
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
            println("A: $lecture B: ${stpappEvents.get(startDate.time)}")
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

            if(lecture.getString("remarks").contains("Prüfung")) {
                //now only god knows
                var examEvent = event.copy()
                examEvent.setDescription(lecture.getString("description"))
                examEvent.setLocation(lecture.getString("room"))
                var examSummary = examEvent.setSummary(lecture.getString("description"))
                examSummary.language = "de-DE"
                cal.events.add(examEvent)
                globalCal.events.add(examEvent)
            }
            println(description)
            cal.events.add(event)
            globalCal.events.add(event)
        }
    }
    return cal
}
fun fetchStpAppLectures(username : String, password : String, group : String, stpappEvents : MutableMap<Long, VEvent>) : Long {
    var startTime = System.currentTimeMillis()
    var resp = Unirest.get("http://stpapp.ba-leipzig.de/model/get/stundenplan.php?passwort=$password&benutzername=$username&seminargruppe=$group")
            .asJson()
            .body
            .`object`
    stpappTime = (System.currentTimeMillis() - startTime) / 1000.0
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

            var timeInMillis = start.time
            minDay = Math.min(timeInMillis, minDay)

            event.setDateStart(start)
            event.setDateEnd(end)
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