package furhatos.app.pizzaorder.nlu

import furhatos.nlu.TextGenerator
import furhatos.util.Language
import furhatos.nlu.*
import furhatos.nlu.common.*
import furhatos.nlu.common.Number
import furhatos.nlu.wikidata.City
import furhatos.nlu.wikidata.Country
import furhatos.records.GenericRecord

open class OrderPizzaIntent : Intent(), TextGenerator {
    //var source : City? = null
    var destination : City? = null
    var date : Date? = null
    var mealChosen: Boolean? = null
    var mealOption: MealOptions? = null
    var topping : ListOfTopping? = null
    var deliverTo : Place? = null
    var travelTime : Time? = null
    var baggage : Int? = null
    var seatingSelection : Boolean? = null
    var seatSide : String? = null
    var seatNumber : Int? = null

    override fun getExamples(lang: Language): List<String> {
        return listOf(
                "I would like to book a ticket",
                "I would like to book",
                "I want to book a ticket",
                "I would like to book a ticket to @destination"
                /*"I would like a pizza to my office at 3 pm",
                "I want a pizza",
                "I want to order a pizza with bacon and ham",
                "Deliver to my @deliverTo instead",
                "I want it to @deliverTo",
                "Deliver it at @deliveryTime",
                "I want it at @deliveryTime",
                "I want to add @topping",
                "I also want @topping",
                "I want @topping",
                "I want it @deliveryTime @deliverTo"*/
        )
    }

    override fun toText(lang : Language) : String {
        if((this.destination != null) and (this.date != null) and (this.travelTime != null) and (this.mealChosen == true)){
            var message = " You're flying [to $destination] on the [$date] at [$travelTime]. "
            if(this.baggage != null && this.baggage == 0){
                message += "You have chosen to not check in baggage. "
            }
            // Not sure why we have to put an extra null-assertion here but not for this.baggage == 0. But it works.
            else if(this.baggage != null && (this.baggage!! > 0))
            {
                message += "You have chosen to check in [$baggage] bags. "
            }
            if(this.seatingSelection == true){
                message += "Your seat is [$seatNumber]-[$seatSide]. "
            }
            else{
                message += "Your seat will be assigned randomly at check-in. "
            }
            if (this.mealOption != null){
                message += "You have also pre-ordered a [$mealOption] meal. "
            }else{
                message += "You have not pre-ordered a meal. "
            }
            return generate(lang, message);
        }else{
            return generate("Let's go ahead!")
        }
    }

    override fun toString(): String {
        return toText()
    }

    override fun adjoin(record: GenericRecord<Any>?) {
        super.adjoin(record)
        /*if (destination != null){
            destination?.list = destination?.list?.distinctBy { it.value }!!.toMutableList()
        }*/
        /*if (topping != null){
            topping?.list = topping?.list?.distinctBy { it.value }!!.toMutableList()
        }*/
    }
}

class  TellDestinationIntent : Intent() {
    var destination : City? = null

    override fun getExamples(lang: Language): List<String> {
        return listOf(
                "I would like to travel to @destination",
                "@destination",
                "I would like to book a ticket to @destination"
        )
    }
}

class IfCountry : Intent (){

    var destination : Country? = null
    override fun getExamples(lang: Language): List<String> {
        return listOf(
                "I would like to travel to @destination",
                "@destination",
                "I would like to book a ticket to @destination"
        )
    }
}

class TellPlaceIntent : Intent() {
    var deliverTo : Place? = null

    override fun getExamples(lang: Language): List<String> {
        return listOf("home", "to my home", "I want it delivered to my home", "send it to my home")
    }
}


class TellTimeIntent(var time : Time? = null) : Intent() {

    override fun getExamples(lang: Language): List<String> {
        return listOf("@time", "at @time")
    }
}

class ToppingIntent : AddToppingIntent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf(
                "@topping",
                "yes @topping")
    }
}

open class AddToppingIntent : Intent() {
    var topping : ListOfTopping? = null

    override fun getExamples(lang: Language): List<String> {
        return listOf(
                "I want @topping",
                "I also want @topping",
                "I want to add @topping"
        )
    }
}

class RemoveToppingIntent : Intent() {
    var topping : ListOfTopping? = null

    override fun getExamples(lang: Language): List<String> {
        return listOf(
            "I want to remove bacon",
            "no bacon",
            "I do not want bacon",
            "I don't want bacon",
            "remove bacon from my pizza")
    }
}



class RequestOptionsIntent : Intent()  {
    override fun getExamples(lang: Language): List<String> {
        return listOf("what options are there",
                "what are the options",
                "what can I choose from",
                "what do you have")
    }
}

class RequestDeliveryOptionsIntent : Intent()  {
    override fun getExamples(lang: Language): List<String> {
        return listOf("where can you deliver",
                "where can I get it")
    }
}

class RequestToppingOptionsIntent : Intent()  {
    override fun getExamples(lang: Language): List<String> {
        return listOf("what topping do you have",
                "what different toppings do you have?")
    }
}

class RequestOpeningHoursIntent : Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf("what are your opening hours",
                "when do you open",
                "when do you close")
    }
}

//Related to seating

class TellSideIntent : Intent() {
    var side : Side? = null

    override fun getExamples(lang: Language): List<String> {
        return listOf( "window", "on window side", "I want it on window side","middle", "on middle side", "I want it on middle side","aisle", "on aisle side", "I want it on aisle side")
    }
}


class RequestSeatSideIntent : Intent()  {
    override fun getExamples(lang: Language): List<String> {
        return listOf("Window",
                "Aisle",
                "Middle")
    }
}

class RequestSideOptionsIntent : Intent()  {
    override fun getExamples(lang: Language): List<String> {
        return listOf("where can I sit",
                "What different seating options do you have?")
    }
}

class RequestSeatNumberOptionsIntent : Intent()  {
    override fun getExamples(lang: Language): List<String> {
        return listOf("Where is available?",
                "What seat numbers do you have available?")
    }
}


class ChangeSeatIntent : Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf(
                "I want to change my seat",
                "Seat",
                "Seating",
                "Change the seat")
    }
}

class ChangeDestinationIntent : Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf(
                "I want to change my destination",
                "Destination",
                "I want to change where I go",
                "Change my flight destination")
    }
}


class ChangeDateIntent : Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf(
                "I want change the date",
                "Date",
                "I want change the flight day",
                "Change the flight day",
                "Day",
                "Change the Date")
    }
}

class ChangeTimeIntent : Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf(
                "I want change the Time",
                "Time",
                "I want change flight hour",
                "Change the flight hour",
                "hour",
                "Change the Time")
    }
}

class ChangeBaggageIntent : Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf(
                "I want to change my baggage selection",
                "Baggage",
                "My baggage",
                "Change the Baggage selection")
    }
}

class ChangeMealIntent : Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf(
                "I want to change my meal selection",
                "meal",
                "food",
                "Change the meal selection")
    }
}

