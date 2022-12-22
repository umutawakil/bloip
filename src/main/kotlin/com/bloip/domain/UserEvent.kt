package com.bloip.domain

import com.bloip.domain.user.User
import com.bloip.repositories.UserEventRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 12/18/22.
 *
 * This is the Genesis super domain object; it does it's own stunts!!!! No setters or getters
 *
 * //TODO: The user events need some sort of key signature since anyone can currently spam the even url with BS.
 */
@Entity
@Table(name = "user_event_log")
class UserEvent : StandardDomainObject {

    object UserEventHelpers {
        var userEventRepository: UserEventRepository? = null
    }

    companion object {
        val eventTimes: MutableMap<String, Date> = ConcurrentHashMap()

        fun findById(id: Long) : UserEvent? {
            return UserEventHelpers.userEventRepository!!.findById(id).orElse(null)
        }

        fun getTimeSinceLastEvent(sequenceId: String) : Float {
            val time: Long = eventTimes[sequenceId]?.time ?: return 0F
            return (Date().time - time) / 1000F
        }
    }

    @Service
    class UserEventService(@Autowired val userEventRepository: UserEventRepository) {
        @PostConstruct
        fun init() {
            UserEventHelpers.userEventRepository = userEventRepository
        }
    }

    @Transient
    private val executorService: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>()
    )

    @Column
    private val name: String

    @Column
    private val methodName: String

    @Column
    private val context: String?

    @Column
    private val duration: Double?

    @Column
    private val url: String?

    @ManyToOne(optional = true)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true)
    private val user: User?

    @Column
    private val sessionId: String?

    @Column
    private val sequenceId: String

    @Column
    private val sequenceComplete: Boolean

    @Column
    private val comment: String?

    @Column
    private var timeSinceLastEvent: Float = 0F

    constructor(
        name: String,
        methodName: String,
        context: String,
        durationInNanoSecs: Double? = null,
        url: String? = null,
        user: User?,
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
        this.user             = user
        this.sessionId        = sessionId
        this.sequenceId       = sequenceId
        this.sequenceComplete = sequenceComplete
        this.comment          = comment
    }

    fun asyncSave() {
        executorService.execute {
            saveNow()
        }
    }

    fun saveNow() : UserEvent {
        this.timeSinceLastEvent = getTimeSinceLastEvent(sequenceId = this.sequenceId)
        if (this.sequenceComplete) {
            eventTimes.remove(this.sequenceId)
        } else {
            eventTimes[this.sequenceId] = Date()
        }
        return UserEventHelpers.userEventRepository!!.save(this)
    }
}