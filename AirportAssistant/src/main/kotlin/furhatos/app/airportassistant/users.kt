package furhatos.app.airportassistant

import furhatos.app.airportassistant.nlu.BookTicketIntent
import furhatos.flow.kotlin.NullSafeUserDataDelegate
import furhatos.records.User

// Associate an order to a user

val User.order by NullSafeUserDataDelegate { BookTicketIntent() }