package furhatos.app.pizzaorder.flow

import furhatos.app.pizzaorder.*
import furhatos.app.pizzaorder.nlu.*
import furhatos.flow.kotlin.*
import furhatos.nlu.common.*
import furhatos.nlu.common.Number
import furhatos.nlu.wikidata.City
import java.time.LocalTime

/*
    General enquiries that we want to be able to handle, as well as an OrderPizzaIntent that is used for initial orders.
 */
val Questions: State = state(Interaction) {
    onResponse<RequestDeliveryOptionsIntent> {
        furhat.say("We can deliver to your home and to your office")
        reentry()
    }

    onResponse<RequestOpeningHoursIntent> {
        furhat.say("We are open between 7 am and 8 pm")
        reentry()
    }

    onResponse<RequestToppingOptionsIntent> {
        furhat.say("We have " + Topping().optionsToText())
        reentry()
    }
}

// Start of interaction
val Start = state(parent = Questions) {
    onEntry {
        furhat.ask("Welcome to Air One. How may I help you?")
    }

    onResponse<OrderPizzaIntent> {
        users.current.order.adjoin(it.intent)
        furhat.say("Ok, you want to book a ticket ${it.intent}")
        goto(CheckOrder)
    }
}

// Form-filling state that checks any missing slots and if so, goes to specific slot-filling states.
val CheckOrder = state {
    onEntry {
        val order = users.current.order
        when {
            order.destination == null -> goto(RequestDestination)
            order.date == null -> goto(RequestDate)
            order.travelTime == null -> goto(RequestTime)

            //Seat Selection Parts
            order.seatingSelection == null -> goto(requestsSeat)
            (order.seatSide == null && order.seatingSelection == true) -> goto(requestSeatSide)
            (order.seatNumber == null && order.seatingSelection == true) -> goto(requestSeatNum)
            
            order.mealChosen == false -> goto(RequestMealOption)

            /*order.topping == null -> goto(RequestTopping)
            order.deliverTo == null -> goto(RequestDelivery)
            order.deliveryTime == null -> goto(RequestTime)*/
            else -> {
                furhat.say("Alright, so you want to book a ticket. $order.")
                goto(ConfirmOrder)
            }
        }
    }
}

/*
    State for handling changes to an existing order
 */
val OrderHandling: State = state(parent = Questions) {

    // Handler that re-uses our pizza intent but has a more intelligent response handling depending on what new information we get
    onResponse<OrderPizzaIntent> {
        val order = users.current.order

        // Message to be constructed based on what data points we get from the user
        var message = "Okay"

        // Adding topping(s) if we get any new
        if (it.intent.topping != null) message += ", adding ${it.intent.topping}"

        // Adding or changing delivery option and time
        if (it.intent.deliverTo != null || it.intent.travelTime != null) {

            /* We are constructing a specific message depending on if we
            get a delivery place and/or time and if this slot already had a value
             */
            when {
                it.intent.deliverTo != null && it.intent.travelTime != null -> { // We get both a delivery place and time
                    message += ", delivering ${it.intent.deliverTo} ${it.intent.travelTime} "
                    if (order.deliverTo != null || order.travelTime != null) message += "instead " // Add an "instead" if we are overwriting any of the slots
                }
                it.intent.deliverTo != null -> { // We get only a delivery place
                    message += ", delivering ${it.intent.deliverTo} "
                    if (order.deliverTo != null) message += "instead " // Add an "instead" if we are overwriting the slot
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

    // Specific handler for removing toppings since this is to complex to include in our OrderPizzaIntent (due to the ambiguity of adding vs removing toppings)
    onResponse<RemoveToppingIntent> {
        users.current.order.topping?.removeFromList(it.intent?.topping!!)
        furhat.say("Okay, we remove ${it.intent?.topping} from your pizza")
        reentry()
    }
}

/*val RequestSource : State = state(parent = OrderHandling) {
    onEntry {
        furhat.ask("Where will you travel from?")
    }

    onResponse<City> {
        furhat.say("Okay, ${it.intent}")
        users.current.order.source = it.intent
        goto(CheckOrder)
    }
}*/

val RequestDestination : State = state(parent = OrderHandling) {
    onEntry {
        furhat.ask("Where would you like to go?")
    }

    onResponse<City> {
        furhat.say("Okay, ${it.intent}")
        users.current.order.destination = it.intent
        goto(CheckOrder)
    }
}

val RequestDate : State = state(parent = OrderHandling) {
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

val requestsSeat : State = state(parent = OrderHandling) {
    onEntry {
        furhat.ask("Would you like to choose your seat now?")
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
        furhat.say ("OK. You will be assigned seat randomly at the check in" )
        users.current.order.seatingSelection = false
        goto(CheckOrder)
    }

}

val requestSeatSide : State = state(parent = OrderHandling) {
    onEntry {

        furhat.ask(" Which side would you like to sit? Window, aisle or middle?")
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

val requestSeatNum : State = state(parent = OrderHandling)
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

val RequestMealOption : State = state(parent = OrderHandling) {
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


// Request toppings
val RequestTopping : State = state(parent = OrderHandling) {
    onEntry {
        furhat.ask("All our pizzas come with tomato and cheese. Do you want any extra topping?")
    }

    onReentry {
        furhat.ask("Do you want any extra topping?")
    }

    onResponse<Yes> {
        furhat.ask("What kind of topping do you want?")
    }

    onResponse<RequestOptionsIntent> {
        raise(RequestToppingOptionsIntent())
    }

    onResponse<No> {
        furhat.say("Okay, no extra topping")
        users.current.order.topping = ListOfTopping()
        goto(CheckOrder)
    }

    onResponse<ToppingIntent> {
        furhat.say("Okay, ${it.intent.topping}")
        users.current.order.topping = it.intent.topping
        goto(CheckOrder)
    }
}

// Request delivery point
val RequestDelivery : State = state(parent = OrderHandling) {
    onEntry {
        furhat.ask("Where do you want it delivered?")
    }

    onResponse<RequestOptionsIntent> {
        raise(it, RequestDeliveryOptionsIntent())
    }

    onResponse<TellPlaceIntent> {
        furhat.say("Okay, ${it.intent.deliverTo}")
        users.current.order.deliverTo = it.intent.deliverTo
        goto(CheckOrder)
    }
}

// Request delivery time
val RequestTime : State = state(parent = OrderHandling) {
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

// Confirming order
val ConfirmOrder : State = state(parent = OrderHandling) {
    onEntry {
        furhat.ask("Does that sound good?")
    }

    onResponse<Yes> {
        goto(EndOrder)
    }

    onResponse<No> {
        goto(ChangeOrder)
    }
}

// Changing order
val ChangeOrder = state(parent = OrderHandling) {
    onEntry {
        furhat.ask("Anything that you like to change?")
    }

    onReentry {
        furhat.ask("I currently have a pizza ${users.current.order}. Anything that you like to change?")
    }

    onResponse<Yes> {
        reentry()
    }

    onResponse<No> {
        goto(EndOrder)
    }
}

// Order completed
val EndOrder = state {
    onEntry {
        furhat.say("Great! Thanks for your order. Goodbye")
        goto(Idle)
    }
}

