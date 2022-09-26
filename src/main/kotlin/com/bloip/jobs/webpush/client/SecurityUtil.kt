package com.bloip.jobs.webpush.client

import org.apache.commons.codec.binary.Base64
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.util.BigIntegers
import java.security.KeyFactory

/**
 * Created by Usman Mutawakil on 9/23/22.
 */
class SecurityUtil {
    companion object {

        val CURVE = "prime256v1"
        val ALGORITHM = "ECDH"

        fun loadPublicKey(encodedPublicKey: String?): java.security.PublicKey? {
            val decodedPublicKey = Base64.decodeBase64(encodedPublicKey)
            return loadPublicKey(decodedPublicKey)
        }

        fun loadPublicKey(decodedPublicKey: ByteArray?): java.security.PublicKey? {
            val keyFactory = KeyFactory.getInstance("ECDSA", "BC")
            val parameterSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec(CURVE)!!
            val curve = parameterSpec.curve
            val point = curve.decodePoint(decodedPublicKey)
            val pubSpec = ECPublicKeySpec(point, parameterSpec)
            return keyFactory.generatePublic(pubSpec)
        }

        fun loadPrivateKey(encodedPrivateKey: String?): java.security.PrivateKey? {
            val decodedPrivateKey = Base64.decodeBase64(encodedPrivateKey)
            return loadPrivateKey(decodedPrivateKey)
        }

        fun loadPrivateKey(decodedPrivateKey: ByteArray?): java.security.PrivateKey? {
            val s = BigIntegers.fromUnsignedByteArray(decodedPrivateKey)
            val parameterSpec: org.bouncycastle.jce.spec.ECParameterSpec =
                ECNamedCurveTable.getParameterSpec(CURVE)

            val privateKeySpec = ECPrivateKeySpec(s, parameterSpec)
            val keyFactory = KeyFactory.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME)
            return keyFactory.generatePrivate(privateKeySpec)
        }

    }
}