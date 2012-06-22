def delayHandler = this.'testDelayer.handler'
def delayedMessageCount = delayHandler.delayedMessageCount

println delayedMessageCount

assert 2 == delayedMessageCount

delayHandler.reschedulePersistedMessages()

return true
