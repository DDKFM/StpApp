package de.ddkfm.stpapp

import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import com.beust.klaxon.Klaxon
import com.mashape.unirest.http.Unirest
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.util.*

fun main(args : Array<String>) {
    val username = args[0]
    val password = args[1]
    val group = args[2]
    var resp = Unirest.get("http://stpapp.ba-leipzig.de/model/get/stundenplan.php?passwort=$password&benutzername=$username&seminargruppe=$group")
            .asJson()
            .body
            .`object`
    println(resp)
    var data = resp.getJSONObject("data")
    var events = data.getJSONObject("termine")
    var lectures = data.getJSONObject("vorlesungen")
    var modules = data.getJSONObject("module")
    var lecturers = data.getJSONObject("dozenten")
    var defaultRoom = data.getString("defaultraum")
    var globalCal = ICalendar()
    var eventCal = ICalendar()
    var mealCal = ICalendar()
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

            println(text)
            var summary = event.setSummary(lecture.getString("name"))
            summary.language = "de-DE"
            event.setDescription(text)
            event.setColor(lecture.getString("farbe"))

            var start = getFullDate(day, time.split(".")[0])
            var end = getFullDate(day, time.split(".")[1])
            event.setDateStart(Date(start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
            event.setDateEnd(Date(end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
            globalCal.addEvent(event)
            eventCal.addEvent(event)
        }
    }
    var mealResp = Unirest.get("http://stpapp.ba-leipzig.de/model/get/essen.php")
            .asJson()
            .body.
            `object`
    data = mealResp.getJSONObject("data")
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
    File("./stpAndMeal.ical").writeText(Biweekly.write(globalCal).go(), Charset.forName("UTF-8"))
    File("./stp.ical").writeText(Biweekly.write(eventCal).go(), Charset.forName("UTF-8"))
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