package com.bloip.domain

import java.io.Serializable
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 6/21/22.
 */
@Entity
@Table(name = "user")
class User : Serializable
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id:Long = 0
}