package furhatos.app.airportassistant.flow

import furhatos.app.airportassistant.*
import furhatos.app.airportassistant.nlu.*
import furhatos.flow.kotlin.*
import furhatos.nlu.common.*
import furhatos.nlu.common.Number
import furhatos.snippets.snippets
import java.time.LocalTime

val Questions: State = state(Interaction) {
    onResponse<RequestSeatNumberOptionsIntent> {
        furhat.say("You can pick between 1 to 24")
        reentry()
    }
}

// Start of interaction
val Start = state(parent = Questions) {
    onEntry {
        val order = users.current.order
        if(order.destination != null && order.date != null && order.travelTime != null && order.baggage != null &&
                    order.seatingSelection != null && order.mealChosen != null)
            reentry()
        else
            furhat.ask("Welcome to Air One. How may I help you?")

    }

    onReentry {
        furhat.ask("Hello again. Do you want to change your ticket?")
    }

    onResponse<TellDestinationIntent> {
        furhat.say("Great, you would like to book a ticket to ${it.intent.destination}")
        users.current.order.destination = it.intent.destination
        goto(CheckOrder)
    }

    onResponse<IfCountry> {
        furhat.say("Please select a city in ${it.intent.destination}")
        goto(CheckOrder)
    }

    onResponse<BookTicketIntent> {
        users.current.order.adjoin(it.intent)
        furhat.say("Ok, you want to book a ticket ${it.intent}")
        goto(CheckOrder)
    }

    onResponse<Yes> {
        goto(ChangeOrder)
    }
    onResponse<No> {
//        furhat.say("Ok. Feel free to come whenever you want to change your booking. Have a nice flight!")
        goto(EndOrder)
    }
}

// Form-filling state that checks any missing slots and if so, goes to specific slot-filling states.
val CheckOrder = state {
    onEntry {
        val order = users.current.order
        when {
            order.destination == null -> goto(RequestDestination)
            order.departure == null -> goto(RequestDeparture)

            order.month == null -> goto(RequestMonth)
            order.day == null -> goto(RequestDay)

            order.baggage == null -> goto(RequestBaggage)

            //Seat Selection Parts
            order.seatingSelection == null -> goto(requestsSeat)
            (order.seatSide == null && order.seatingSelection == true) -> goto(requestSeatSide)
            (order.seatNumber == null && order.seatingSelection == true) -> goto(requestSeatNum)

            order.mealChosen == null -> goto(RequestMealOption)

            else -> {
                furhat.say("$order.") // It is annoying to repeat if user changes
                goto(ConfirmOrder)
            }
        }
    }
}

/*
    State for handling changes to an existing order
 */
val BookHandling: State = state(parent = Questions) {

    // Handler that re-uses our pizza intent but has a more intelligent response handling depending on what new information we get
    onResponse<BookTicketIntent> {
        val order = users.current.order

        // Message to be constructed based on what data points we get from the user
        var message = "Okay"

        // Adding or changing delivery option and time
        if (it.intent.departure != null || it.intent.travelTime != null) {

            /* We are constructing a specific message depending on if we
            get a delivery place and/or time and if this slot already had a value
             */
            when {
                it.intent.departure != null && it.intent.travelTime != null -> { // We get both a delivery place and time
                    message += ", delivering ${it.intent.departure} ${it.intent.travelTime} "
                    if (order.departure != null || order.travelTime != null) message += "instead " // Add an "instead" if we are overwriting any of the slots
                }
                it.intent.departure != null -> { // We get only a delivery place
                    message += ", delivering ${it.intent.departure} "
                    if (order.departure != null) message += "instead " // Add an "instead" if we are overwriting the slot
                }
                it.intent.travelTime != null -> { // We get only a delivery time
                    message += ", delivering ${it.intent.travelTime} "
                    if (order.travelTime != null) message += "instead " // Add an "instead" if we are overwriting the slot
                }
            }
        }

        // Deliver our message
        furhat.say(message)

        // Finally we join the existing order with the new one
        order.adjoin(it.intent)

        reentry()
    }
}


val RequestDestination : State = state(parent = BookHandling) {
    onEntry {
        furhat.ask("Where would you like to go?")
    }

    onReentry {
        furhat.ask("Which city would you like to travel to ?")
    }

    onResponse<TellDestinationIntent> {
        furhat.say("Great, ${it.intent.destination}")
        users.current.order.destination = it.intent.destination
        goto(CheckOrder)

    }


    onResponse<IfCountry> {
        furhat.say("You will have to be more specific by selecting a city in ${it.intent.destination}")
        reentry()
    }

    onNoResponse {
        furhat.ask("I did not understand, kindly could you please repeat?")
    }
}

val RequestDeparture : State = state(parent = BookHandling) {
    onEntry {
        furhat.ask("Where will you travel from? Currently there are only two options. ${Place().optionsToText()} ")
    }

    onReentry {
        furhat.ask("There are only, two avaliable options. ${Place().optionsToText()}")
    }

    onResponse<TellDepartureIntent> {
        furhat.say("Okay, ${it.intent.departure}")
        users.current.order.departure = it.intent.departure
        goto(CheckOrder)
    }
}

val RequestMonth : State = state(parent = BookHandling) {
    onEntry {
        furhat.ask("What month would you like to travel?")
    }

    onReentry {
        furhat.ask("Please specify a month.")
    }

    onResponse<TellMonthIntent> {
        furhat.say("${it.intent.month} it is.")
        users.current.order.month = it.intent.month
        goto(CheckOrder)

    }

    onNoResponse {
        furhat.ask("I am sorry, I must have missed what you said. Would you be so kind as to repeat?")
    }
}

val RequestDay : State = state(parent = BookHandling) {
    onEntry {
        furhat.ask("What day would you like to travel?")
    }

    onReentry {
        furhat.ask("Please specify a day.")
    }

    onResponse<TellDayIntent> {

        val day_int = it.intent.day.toString().dropLast(2).toInt() // Convert ordinal to integer except 1st/2nd/3rd
        val available = arrayOf<Int>(4, 6, 9, 12, 14, 17, 20, 22, 24, 27, 29, 31)
        val rndhour = (11..23).random()
        val rndmin = (10..58).random()
        users.current.order.hour = rndhour // Set here already since they will be sent back to RequestDay if they disagree
        users.current.order.min = rndmin
        if (day_int in available) {
            furhat.say("On that day there is a flight at $rndhour $rndmin o'clock.")
            users.current.order.day = it.intent.day
            goto(DayAccept)
        } else if (day_int + 1 in available) {
            furhat.say("Unfortunately, there are no flights available on that day. However, there is one on the ${day_int + 1}th at ${rndhour} ${rndmin} o'clock.")
            users.current.order.day = it.intent.day
            goto(DayAccept)
        } else {
            furhat.say("Unfortunately, there are no flights available on that day. However, there is one on the ${day_int - 1}th at ${rndhour} ${rndmin} o'clock.")
            users.current.order.day = it.intent.day
            goto(DayAccept)
        }

    }

    onNoResponse {
        furhat.ask("My sincerest apologies, I must have missed what you said. Would you be so kind as to reiterate?")
    }
}

val DayAccept : State = state(parent = BookHandling) {

    onEntry {
        furhat.ask("Would this be acceptable?")
    }

    onResponse<Yes> {
        furhat.say("Excellent.")
        goto(CheckOrder)
    }
    onResponse<No> {
        furhat.say ("Ok. Please consider another day on which to travel.")
        goto(RequestDay)
    }
}

val RequestDate : State = state(parent = BookHandling) {
    onEntry {
        furhat.ask("What date would you like to travel?")
    }

    onReentry {
        furhat.ask("When would you like to travel?")
    }

    onResponse<Date> {
        furhat.say("Okay, ${it.intent}")
        users.current.order.date = it.intent
        goto(CheckOrder)
    }
}

val requestsSeat : State = state(parent = BookHandling) {
    onEntry {
        furhat.ask("Would you like to choose your seat?")
    }

    onReentry {
        furhat.say("Would you like to change your decision about seating?")
    }

    onResponse<Yes> {
        furhat.say("Alright")
        users.current.order.seatingSelection = true
        goto(CheckOrder)
    }
    onResponse<No> {
        furhat.say ("OK. You will be assigned seat randomly at the gate" )
        users.current.order.seatingSelection = false
        goto(CheckOrder)
    }

}

val requestSeatSide : State = state(parent = BookHandling) {
    onEntry {

        furhat.ask(" Which side would you like to sit? We have ${Side().optionsToText()}")
    }

    onReentry {
        furhat.ask("Which side do you want to sit?")
    }

    onResponse<RequestOptionsIntent> {
        raise(it, RequestSideOptionsIntent())
    }

    //Add random answer
    onResponse<TellSideIntent> {
        when {
            it.intent.side!!.value ==  "Window" -> users.current.order.seatSide = "A"
            it.intent.side!!.value == "Middle" -> users.current.order.seatSide = "B"
            it.intent.side!!.value == "Aisle" -> users.current.order.seatSide = "C"
        }

        furhat.say("Okay, ${it.intent.side} also ${users.current.order.seatSide}")
        goto(CheckOrder)
    }
}

val requestSeatNum : State = state(parent = BookHandling)
{
    onEntry {
        furhat.ask("Which Seat number would you like to sit? Select between 1 to 24")
    }

    onReentry {
        furhat.ask("Please pick a seat between 1 and 24")
    }

    onResponse<RequestOptionsIntent> {
        raise(it, RequestSeatNumberOptionsIntent())
    }

    //Add random answer

    onResponse<Number> {
        var num = it.intent.value
        if (num != null) {
            if(num in 1..24) {
                var res = num.toString() + users.current.order.seatSide
                furhat.say("Okay, you selected seat ${it.intent.value} .")
                users.current.order.seatNumber = it.intent.value
            }
            else
            {
                furhat.say("That is an invalid selection")
            }
            goto(CheckOrder)

        }

    }
}

val RequestMealOption : State = state(parent = BookHandling) {
    onEntry {
        furhat.ask("Would you like to pre-order a meal?")
    }

    onResponse<Yes> {
        furhat.say("Great! You can choose from the following options - ${MealOptions().optionsToText()}?")
        furhat.ask("What would you like to choose?")
    }

    onReentry {
        furhat.ask("Would you like to pre-order a meal?")
    }

    onResponse<No> {
        furhat.say("Okay, no meal order")
        users.current.order.mealChosen = true
        goto(CheckOrder)
    }
    onResponse<MealOptions> {
        furhat.say("Okay, ${it.intent}")
        users.current.order.mealChosen = true
        users.current.order.mealOption = it.intent
        goto(CheckOrder)
    }
}

// Request delivery time
val RequestTime : State = state(parent = BookHandling) {
    onEntry {
        furhat.ask("At what time do you want to travel?")
    }

    /*onResponse<RequestOptionsIntent> {
        raise(RequestOpeningHoursIntent())
    }*/

    onResponse<Number> {
        var hour = it.intent.value
        if (hour != null) {
            // We're assuming we want an afternoon delivery, so if the user says "at 5", we assume it's 5pm.
            if (hour <= 12) hour += 12
            raise(it, TellTimeIntent(Time(LocalTime.of(hour, 0))))
        }
        else {
            propagate()
        }
    }

    onResponse<TellTimeIntent> {
        furhat.say("Okay, ${it.intent.time}")
        users.current.order.travelTime = it.intent.time
        goto(CheckOrder)
    }
}

// Request baggage
val RequestBaggage : State = state(parent = BookHandling) {
    onEntry {
        random(
                {
                    furhat.ask("Do you want to check in any bags?")
                    furhat.ask("Would you like to check in baggage?")
                }
        )
    }

    onReentry {
        furhat.ask("Do you have any baggage to check in?")
    }

    onResponse<Yes> {
        furhat.ask("How many bags do you want to check in? You may bring minimum 1 bag and maximum 3 bags.")
    }


    onResponse<No> {
        furhat.say("Ok, so you choose to not bring any bags.")
        users.current.order.baggage = 0
        goto(CheckOrder)
    }

    // We assume that the volume of each bag is within the limit
    onResponse<Number> {
        var numBaggage: Int? = it.intent.value
        var maxBags = 4
        var minBags = 1
        var maxWeight = 8.5
        var minWeight = 1.5

        if (numBaggage != null) {
            if (numBaggage > maxBags || numBaggage < 0) {
                snippets {
                    furhat.say("Sorry, you can not bring $numBaggage bags. You are only allowed to bring minimum $minBags bag and maximum $maxBags bags.")
                    reentry()
                }
            }
            else {

                furhat.say("Ok, you choose to check in $numBaggage bags.")
                furhat.say("I will weigh your bags now.")
                users.current.order.baggage = numBaggage
                var extraPrice: Int = 0
                for (i in minBags..numBaggage)
                {
                    val weight = minWeight + Math.random()*(maxWeight - minWeight)
                    furhat.say("Bag number $i weights " + String.format("%.2f", weight) + " kg.")
                    if (weight > 4)
                    {
                        // We assume that the user accepts the extra price
                        furhat.say("This bag weights more than 4 kg. You will have to pay $10 extra for this bag.")
                        extraPrice++
                    }
                }

                val baggagePrice = 20*numBaggage + 10*extraPrice
                furhat.say("The total cost for your baggage will be $baggagePrice.")
            }
        }
        else{
            propagate()
        }
        goto(CheckOrder)
    }

}

// Confirming order
val ConfirmOrder : State = state(parent = BookHandling) {
    onEntry {
        furhat.ask("Does that sound good?")
    }

    onResponse<Yes> {
        furhat.say("Great! Thanks for your booking.")
        goto(EndOrder)
    }

    onResponse<No> {
        goto(ChangeOrder)
    }
}

// Changing order
val ChangeOrder = state(parent = BookHandling) {
    onEntry {
        furhat.ask("What do you want to change?")
    }

    onReentry {
        furhat.ask(" ${users.current.order}. Anything that you like to change?")
    }

    onResponse<Yes> {
        reentry()
    }

    onResponse<ChangeSeatIntent> {
        users.current.order.seatingSelection = null
        users.current.order.seatSide = null
        users.current.order.seatNumber = null
        furhat.say("Alright. Directing you to seating selection.")
        goto(CheckOrder)
    }

    onResponse<ChangeDestinationIntent> {
        val order = users.current.order
        users.current.order.destination = null
        order.month = null
        order.day = null
        order.baggage = null
        order.seatingSelection = null
        order.seatSide = null
        order.seatNumber = null
        order.mealChosen = null

        furhat.say("Alright. Directing you to destination selection.")
        goto(CheckOrder)
    }

    onResponse<ChangeDateIntent> {
        val order = users.current.order
        order.month = null
        order.day = null
        order.baggage = null
        order.seatingSelection = null
        order.seatSide = null
        order.seatNumber = null
        order.mealChosen = null
        furhat.say("Alright. Directing you to Date selection.")
        goto(CheckOrder)
    }

//    onResponse<ChangeTimeIntent> {
//        users.current.order.travelTime = null
//        furhat.say("Alright. Directing you to travel time selection.")
//        goto(CheckOrder)
//    }

    onResponse<ChangeBaggageIntent> {
        users.current.order.baggage = null
        furhat.say("Alright. Directing you to baggage selection.")
        goto(CheckOrder)
    }

    onResponse<ChangeMealIntent> {
            users.current.order.mealChosen = null
            users.current.order.mealOption = null

            furhat.say("Alright. Directing you to meal selection.")
            goto(CheckOrder)
        }

    onResponse<No> {
        goto(EndOrder)
    }
}

// Order completed
val EndOrder = state {
    onEntry {
        furhat.say("Feel free to come whenever you want to change your booking. Have a nice flight!")
        goto(Idle)
    }
}

