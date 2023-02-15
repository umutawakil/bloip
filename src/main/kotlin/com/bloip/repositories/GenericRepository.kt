package com.bloip.repositories

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.discussion.Discussion
import org.hibernate.Session
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory

/**
 * //TODO: Whats the best way to remove the custom queries and replace them with code or at least centralize them?
 */
@Component
class GenericRepository (
    @Autowired private  val entityManagerFactory: EntityManagerFactory,
    @Autowired private val applicationProperties: ApplicationProperties,
) {
    fun<T> findById(targetClass: Class<T>, id: Any) : T? {
       val entityManager: EntityManager = entityManagerFactory.createEntityManager()
       try {
           return entityManager.find(targetClass, id )
       } finally {
           entityManager.close()
       }
    }

    fun<T> findAll(targetClass: Class<T>) : List<T> {
        val entityManager: EntityManager = entityManagerFactory.createEntityManager()
        try {
            return entityManager.createQuery(
                "SELECT e FROM " + targetClass.simpleName + " e").resultList as List<T>
        } finally {
            entityManager.close()
        }
    }

    fun <T> findAllBy(query: String,targetClass: Class<T>) : List<T> {
        val entityManager: EntityManager = entityManagerFactory.createEntityManager()
        try {
            return entityManager.createQuery(query, targetClass).resultList as List<T>
        } finally {
            entityManager.close()
        }
    }

    fun <T> delete(entity: T, targetClass: Class<T>) {
        val entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val session: Session = entityManager.unwrap(Session::class.java)
        val tx = session.beginTransaction()
        try {
            session.delete(session.merge(entity))
            tx.commit()
        } finally {
            session.close()
        }
    }

    fun <T> save(input: T) : T {
        val entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val session: Session = entityManager.unwrap(Session::class.java)
        val tx = session.beginTransaction()
        try {
            val result = entityManager.merge(input)
            tx.commit()
            return result
        } finally {
            session.close()
        }
    }

    /** Requires a transaction and to do get a transaction without using the container we have to unwrap the manager and use hibernate directly, Seems
     * the JPA spec forces you to rely on the containers built in transaction management. In the case of spring this would be @Transactional which
     * I do not want to use.
     */
    fun<T> deleteAll(targetClass: Class<T>) : Int {
        if (applicationProperties.environment != "dev") {
            throw RuntimeException("Function can only be used in dev, specifically for integration testing!!!")
        }
        val entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val session: Session = entityManager.unwrap(Session::class.java)
        val tx = session.beginTransaction()
        try {
            val query = entityManager.createQuery("DELETE FROM " + targetClass.simpleName)
            val result = query.executeUpdate()
            tx.commit()
            return result

        } finally {
            session.close()
        }
    }
}