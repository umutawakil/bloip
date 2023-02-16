package com.bloip.domain

import com.bloip.domain.user.User
import com.bloip.repositories.GenericRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.Column
/**
 * Created by Usman Mutawakil on 12/18/22.
 *
 * This is the Genesis super domain object; it does it's own stunts!!!! No setters or getters
 *
 * //TODO: The user events need some sort of key signature since anyone can currently spam the even url with BS.
 */
@Entity(name= "UserEvent")
@Table(name = "user_event_log")
class UserEvent : StandardDomainObject {

    @Component
    class SpringAdapter (@Autowired var genericRepository: GenericRepository) {
        @PostConstruct
        fun init() {
            UserEvent.genericRepository = genericRepository
        }
    }

    companion object {
        private var conversionRequests = 0
        private var lastPendingEvent: UserEvent? = null
        private val eventTimes: MutableMap<String, Date> = ConcurrentHashMap()
        private lateinit var genericRepository: GenericRepository
        private val eventsByUserId: MutableMap<User.UserId, MutableList<UserEvent>> = ConcurrentHashMap()

        fun clear(userId: User.UserId) {
            if(lastPendingEvent?.userId == userId) {
                lastPendingEvent = null
            }
            val events = eventsByUserId[userId] ?: return
            synchronized(events) {
                for (e in events) {
                    genericRepository.delete(entity = e, targetClass = UserEvent::class.java)
                }
                eventsByUserId[userId]?.clear()
            }
        }

        fun deleteAll() {
            lastPendingEvent = null
            conversionRequests = 0
            eventTimes.clear()
            eventsByUserId.clear()
            genericRepository.deleteAll(targetClass = UserEvent::class.java)
        }

        /*fun findByUserIdAndName(userId: User.UserId, name: String) : List<UserEvent> {
            return eventsByUserId[userId] ?.filter { it.name == name } ?: emptyList()
        }*/

        private fun getTimeSinceLastEvent(sequenceId: String) : Float {
            val time: Long = eventTimes[sequenceId]?.time ?: return 0F
            return (Date().time - time) / 1000F
        }

        fun incrementConversionRequestCountIfApplicable() {
            if(isLastPendingEventConversionRequest()) {
                conversionRequests++
            }
        }
        fun numOfConversionRequests() : Int {
            return conversionRequests
        }
        private fun isLastPendingEventConversionRequest() : Boolean {
            return lastPendingEvent?.name == "conversion_request"
        }
    }

    @Transient
    private val executorService: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>()
    )

    @Column
    private val name: String

    @Column(name="method_name")
    private val methodName: String

    @Column
    private val context: String?

    @Column
    private val duration: Double?

    @Column
    private val url: String?

    @Column(name = "user_id")
    private val userId: User.UserId?

    @Column(name="session_id")
    private val sessionId: String?

    @Column(name="sequence_id")
    private val sequenceId: String

    @Column(name="sequence_complete")
    private val sequenceComplete: Boolean

    @Column
    private val comment: String?

    @Column(name="time_since_last_event")
    private var timeSinceLastEvent: Float = 0F

    constructor(
        name: String,
        methodName: String,
        context: String,
        durationInNanoSecs: Double? = null,
        url: String? = null,
        userId: User.UserId?,
        sessionId: String? = null,
        sequenceId: String,
        comment: String? = null,
        sequenceComplete: Boolean,
    ) {
        this.name             = name
        this.methodName       = methodName
        this.context          = context
        this.duration         = if (durationInNanoSecs != null) { durationInNanoSecs / 1000000.0 } else { null}
        this.url              = url
        this.userId           = userId
        this.sessionId        = sessionId
        this.sequenceId       = sequenceId
        this.sequenceComplete = sequenceComplete
        this.comment          = comment
    }

    fun asyncSave() {
        lastPendingEvent = this
        incrementConversionRequestCountIfApplicable()

        executorService.execute {
            saveNow()
        }
    }

    private fun saveNow() : UserEvent {
        this.timeSinceLastEvent = getTimeSinceLastEvent(sequenceId = this.sequenceId)
        if (this.sequenceComplete) {
            eventTimes.remove(this.sequenceId)
        } else {
            eventTimes[this.sequenceId] = Date()
        }

        val eventUpdated = genericRepository.save(this)
        if(this.userId != null) {
            eventsByUserId.computeIfAbsent(eventUpdated.userId!!) { mutableListOf() }.add(eventUpdated)
        }
        return eventUpdated
    }
}