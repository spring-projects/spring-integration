import org.springframework.integration.test.util.TestUtils
import org.springframework.integration.handler.DelayHandler

def delayHandler = TestUtils.getPropertyValue(testDelayer, 'handler', DelayHandler)

def delayedMessageCount = delayHandler.delayedMessageCount
assert 2 == delayedMessageCount

delayHandler.reschedulePersistedMessages()

return true
