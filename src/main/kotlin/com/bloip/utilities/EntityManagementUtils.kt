package com.bloip.utilities

import com.bloip.configuration.ApplicationProperties
import org.hibernate.Session
import org.hibernate.SessionFactory
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory

/**
 * Created by Usman Mutawakil on 1/2/23.
 */
class EntityManagementUtils {
    companion object {
        var sessionFactory: SessionFactory? = null
        var entityManager: EntityManager?    = null
        var applicationProperties: ApplicationProperties? = null

        fun init(applicationProperties: ApplicationProperties, entityManager: EntityManager) {
            this.applicationProperties = applicationProperties
            this.entityManager         = entityManager
            this.sessionFactory        = entityManager.unwrap(Session::class.java).sessionFactory
        }

        fun runInTransaction(task: () -> Any) : Any {
            val session: Session = sessionFactory!!.openSession()
            val tx = session.beginTransaction()
            try {
                val result = task()
                tx.commit()

                return result
            } finally {
                session.close()
            }
        }

        fun getSession(entityManagerFactory: EntityManagerFactory) : Session {
            return  entityManagerFactory.
            createEntityManager().
            unwrap(Session::class.java).
            sessionFactory.
            openSession()
        }
    }
}