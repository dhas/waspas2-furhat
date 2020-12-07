package furhatos.app.pizzaorder.nlu

import furhatos.nlu.ComplexEnumEntity
import furhatos.nlu.EnumEntity
import furhatos.nlu.ListEntity
import furhatos.nlu.common.Number
import furhatos.util.Language
import kotlin.sequences.Sequence

class ListOfTopping : ListEntity<Topping>()

class Topping : EnumEntity(speechRecPhrases = true) {

    override fun getEnum(lang: Language): List<String> {
        return listOf("onion", "ham", "mozzarella", "bacon", "rocket salad:rocket salad,rocket sallad", "pepper")
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
