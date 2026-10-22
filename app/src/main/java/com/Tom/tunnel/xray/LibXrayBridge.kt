package com.tom.tunnel.xray

import org.json.JSONObject

/**
 * Bridge to the official XTLS/libXray gomobile AAR.
 * The official AAR is built from source by scripts/build-xray-android.sh.
 * Reflection avoids breaking IDE sync before that generated native artifact exists.
 */
object LibXrayBridge {
    private const val API_VERSION = 3

    fun invoke(method: String, payload: JSONObject = JSONObject()): JSONObject {
        val request = JSONObject()
            .put("apiVersion", API_VERSION)
            .put("method", method)
            .put("payload", payload)
            .toString()

        val clazz = try {
            Class.forName("libXray.LibXray")
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException(
                "Moteur natif absent. Lancez scripts/build-xray-android.sh pour générer l'AAR officiel libXray.",
                e
            )
        }

        val invokeMethod = clazz.methods.firstOrNull {
            it.name.equals("invoke", ignoreCase = true) &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == String::class.java
        } ?: throw IllegalStateException("API invoke(String) introuvable dans l'AAR libXray fourni.")

        val raw = invokeMethod.invoke(null, request) as? String
            ?: throw IllegalStateException("Réponse vide du moteur Xray.")
        val response = JSONObject(raw)
        if (!response.optBoolean("success", false)) {
            throw IllegalStateException(response.optString("error", "Erreur Xray inconnue"))
        }
        return response
    }

    fun validate(xrayJson: String) = invoke(
        "testXray",
        JSONObject().put("xrayJson", xrayJson)
    )

    fun start(xrayJson: String) = invoke(
        "runXray",
        JSONObject().put("xrayJson", xrayJson)
    )

    fun stop() = invoke("stopXray")

    fun state(): JSONObject = invoke("getXrayState")
    fun version(): JSONObject = invoke("xrayVersion")
}
