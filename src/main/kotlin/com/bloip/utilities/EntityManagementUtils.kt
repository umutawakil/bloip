package com.bloip.utilities

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.StandardDomainObject
import org.hibernate.LockMode
import org.hibernate.ReplicationMode
import org.hibernate.Session
import org.hibernate.SessionFactory
import javax.persistence.EntityManager

/**
 * Created by Usman Mutawakil on 1/2/23.
 */
class EntityManagementUtils {
    companion object {
        var sessionFactory: SessionFactory? = null

        fun <T>saveToDatabase(entity:T, entityManager: EntityManager, applicationProperties: ApplicationProperties) : T {
            sessionFactory(
                entityManager = entityManager,
                applicationProperties = applicationProperties
            ).use { session ->
                val tx = session.beginTransaction()
                try {
                    if((entity as StandardDomainObject).id == -1L) {
                        session.saveOrUpdate(entity)
                    } else {
                        println("Entity_ID: " + (entity as StandardDomainObject).id)
                        //session.lock(entity, LockMode.NONE)
                        session.update(entity)
                        //session.replicate(entity, ReplicationMode.OVERWRITE)
                    }
                    session.flush()
                    session.refresh(entity)
                    tx.commit()
                    return entity

                } catch (exception: Exception) {
                    exception.printStackTrace()
                    tx?.rollback()
                    throw RuntimeException(exception)
                }
            }
        }

        fun deleteFromDatabase(entity:Any, entityManager: EntityManager, applicationProperties: ApplicationProperties) {
            sessionFactory(entityManager = entityManager, applicationProperties = applicationProperties).use { session ->
                val tx = session.beginTransaction()
                try {
                    session.delete(
                        session.merge(entity)
                    )
                    //session.flush()
                    tx.commit()
                } catch (exception: Exception) {
                    tx.rollback()
                    throw RuntimeException(exception)
                } finally {
                }
            }
        }

        fun sessionFactory(entityManager: EntityManager, applicationProperties: ApplicationProperties) : Session {
            sessionFactory = getSessionFactory(entityManager, applicationProperties)!!

            /*if (sessionFactory!!.isOpen) {
                return sessionFactory!!.currentSession
            }*/
            return sessionFactory!!.openSession()
        }

        private fun getSessionFactory(entityManager: EntityManager, applicationProperties: ApplicationProperties) : SessionFactory {
            if (sessionFactory != null) return sessionFactory!!

            sessionFactory = entityManager.unwrap(Session::class.java).sessionFactory!!
            sessionFactory!!.properties["hibernate.dialect"] = applicationProperties.selectedHbmDialect
            sessionFactory!!.properties["hibernate.current_session_context_class"] = "org.springframework.orm.hibernate5.SpringSessionContext"

            return sessionFactory!!
        }
    }
}