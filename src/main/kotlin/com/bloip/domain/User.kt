package com.bloip.domain

import javax.persistence.*

/**
 * Created by Usman Mutawakil on 6/21/22.
 */
@Entity
@Table(name = "user")
class User
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id:Long? = null

    @Column(name = "email")
    var email:String? = null
}