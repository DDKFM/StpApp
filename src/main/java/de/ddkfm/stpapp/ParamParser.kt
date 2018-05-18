package de.ddkfm.stpapp

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

class ParamParser(parser : ArgParser) {
    val stpappUsername by parser.storing("-u", "--stpapp-username",
            help = "username for stpapp")

    val stpappPasswd by parser.storing("-p", "--stpapp-password",
            help = "password for stpapp")

    val stpappGroup by parser.storing("-g", "--group",
            help = "group for stpapp")

    val campusDualMatriculationNumber by parser.storing("-m", "--campusdual-uid",
            help = "MatriculationNumber(UID) for Campus Dual")

    val campusDualHash by parser.storing("-c", "--campusdual-hash",
            help = "Hash for Campus Dual")

    val icalOutputPath by parser.storing("-i", "--ical-output-path",
            help = "output path for ical-files").default(".")

    val chartOutputPath by parser.storing("-k", "--chart-output-path",
            help = "output path for chart.html").default("./chart")

    val times by parser.storing("-t", "--times-path",
            help = "path to times.txt").default("./times.txt")

    val withChart by parser.flagging("-v", "--withChart",
            help = "should the chart be generated").default(false)
}