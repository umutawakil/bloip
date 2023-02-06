package com.bloip.sharpen

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import org.junit.jupiter.api.Test
import java.security.*
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.util.*


/**
 * Created by Usman Mutawakil on 9/20/22.
 */
class SharpenSwordTest {

    /*@Test
    fun blankSlate() {
        simulateJWTProcess()
    }*/

    /** User this or the method below it to create a new private key of a given size and algorithm **/
    private fun generatePrivateKey() {
        val keyGen: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(256)

        val keyPair: KeyPair         = keyGen.genKeyPair()
        val privateKey: PrivateKey   = keyPair.private

        println("PrivateKey: " + Base64.getEncoder().encodeToString(privateKey.encoded))
    }
    private fun generatePrivateKeyString () : String {
        val keyGen: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)

        val keyPair: KeyPair         = keyGen.genKeyPair()
        val privateKey: PrivateKey   = keyPair.private

        return Base64.getEncoder().encodeToString(privateKey.encoded)
    }

    /** Use the pre-generated or hardcoded private key string to run this process. The hardcoded string could be generated using the methods above **/
    private fun simulateJWTProcess() {
        val privateKeyString = "{Put private key here}"
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString.encodeToByteArray()))
        val privateKey: PrivateKey = keyFactory.generatePrivate(privateKeySpec)

        val publicKeySpec = RSAPublicKeySpec((privateKey as RSAPrivateKey).modulus, (privateKey as RSAPrivateCrtKey).publicExponent)
        val publicKey: PublicKey = keyFactory.generatePublic(publicKeySpec)

        val algorithm = Algorithm.RSA256(publicKey as RSAPublicKey, privateKey)
        val token = JWT.create()
            .withIssuer("bloip")
            .withClaim("userId","22")
            .withClaim("email","test@dev.bloip.com")
            .sign(algorithm)

        val verifier: JWTVerifier = JWT.require(algorithm)
            .withIssuer("bloip")
            .build()

        try {
            val decodedJWT: DecodedJWT = verifier.verify("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJibG9pcCIsInVzZXJJZCI6IjQzNDUifQ.Jg3HCrXPj7jdmNDa8t3WpigrYpF3K7v4js9Cc1k-p592upjN-yjL0asfqP6rSm1TcrTC9ZUpWy1P_FqvUvEIPak_yc4-8IrAr6m4p1giqetJ3ZgaeMEhpQzCD6Lpvtz-5HcmygdneqaiikNZZ1z34JgNbDkhSU0Z6t4U_LF-9h-gvl_snaUpjJrnAiWuyGHzHpQnpNGR_lnDG5Nfm6tGGTWa3YssuZtP20ZOIwpqTvhP-qv1HqryYGuzaONFRVT942TxTW-A_pReCIwILFXnXXWeSvXNAeuUNPkrMvD_iNMCxwr2tjJuHWBiTNBvhif5q4jL1lJBkYDCFLDt6kGg")
            println("UserID: " + decodedJWT.getClaim("userId"))
            println("EMAIL: " + decodedJWT.getClaim("email"))

        } catch (e:JWTVerificationException) {
            println("Bad JWT")
        }
    }


    @Test
    fun can_find_index_of_string() {
        val x = "<div>To <a href=\"XXXXXX\">unsubscribe</a> from these emails click here -> <a href=\"AXXXXXXXB\">Unsubscribe</a></div>"
        val start = "click here -> <a href=\""
        val stop  = "\">Unsubscribe</a></div>"
        val positionX = x.indexOf(start) + start.length
        val positionY = x.indexOf(stop)

        println (x.substring(positionX, positionY))
    }
}