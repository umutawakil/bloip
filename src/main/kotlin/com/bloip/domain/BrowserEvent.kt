package com.bloip.domain

import com.bloip.repositories.GenericRepository
import com.bloip.services.admin.AdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.Column

@Entity(name= "BrowserEvent")
@Table(name = "browser_event")
class BrowserEvent : StandardDomainObject {
    @Component
    class SpringAdapter (
        @Autowired var genericRepository: GenericRepository,
        @Autowired var adminService: AdminService
        ) {
        @PostConstruct
        fun init() {
            BrowserEvent.genericRepository = genericRepository
            BrowserEvent.adminService      = adminService
        }
    }

    companion object {
        private lateinit var genericRepository: GenericRepository
        private lateinit var adminService: AdminService
    }

    @Transient
    private val executorService: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>()
    )

    @Column
    private val name: String
    @Column
    private val value: String
    @Column
    private val browserInfo: String

    constructor(
        name: String, value: String, browserInfo: String
    ) {
        this.name        = name
        this.value       = value
        this.browserInfo = browserInfo
    }

    fun asyncSave() {
        executorService.execute {
            saveNow()
        }
    }

    private fun saveNow() : BrowserEvent {
        try {
            adminService.recordEvent(
                eventMessage = "New user browser event"
            )
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        return genericRepository.save(this)
    }
}