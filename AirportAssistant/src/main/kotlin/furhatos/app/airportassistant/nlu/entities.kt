package furhatos.app.airportassistant.nlu

import furhatos.nlu.EnumEntity
import furhatos.util.Language


class Month : EnumEntity(speechRecPhrases = true) {

    override fun getEnum(lang: Language): List<String> {
        return listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    }

}

class MealOptions : EnumEntity(speechRecPhrases = true) {

    override fun getEnum(lang: Language): List<String> {
        return listOf("Vegetarian", "Vegan", "Fish", "Chicken")
    }

}

class BaggageOptions : EnumEntity(speechRecPhrases = true) {

    override fun getEnum(lang: Language): List<String> {
        return listOf("Regular", "Large")
    }

}

class Side : EnumEntity() {
    override fun getEnum(lang: Language): List<String> {
        return listOf("Window", "Middle", "Aisle")
    }
}

class Place : EnumEntity() {

    override fun getEnum(lang: Language): List<String> {
        return listOf("Stockholm", "Gothenburg")
    }

    /*// Method overridden to produce a spoken utterance of the place
    override fun toText(lang: Language): String {
        return generate(lang,"to your $value")
    }*/
}
