package furhatos.app.airportassistant

import furhatos.app.airportassistant.flow.Idle
import furhatos.skills.Skill
import furhatos.flow.kotlin.*

class AirportAssistantSkill : Skill() {
    override fun start() {
        Flow().run(Idle)
    }
}

fun main(args: Array<String>) {
    Skill.main(args)
}
