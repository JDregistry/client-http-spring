package jdregistry.client.impl.http.spring

import jdregistry.client.http.IHttpGetClient
import jdregistry.client.http.IHttpResponse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.WWW_AUTHENTICATE
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * Implements the [IHttpGetClient] interface using the Web module of the Spring Framework.
 *
 * More concrete, the [RestTemplate] class is used to implement the logic of [get]
 *
 * @author Lukas Zimmermann
 * @see IHttpGetClient
 * @since 0.0.4
 *
 */
class SpringHttpGetClient : IHttpGetClient {

    private val restTemplate = RestTemplate()

    init {

        restTemplate.errorHandler = object : DefaultResponseErrorHandler() {

            override fun hasError(response: ClientHttpResponse): Boolean {

                // In this implementation, we do not treat 401 as error, since
                // the Docker Registry might initiate an authentication challenge using
                // this response handler
                val statusCode = response.statusCode
                return statusCode != HttpStatus.OK && statusCode != HttpStatus.UNAUTHORIZED
            }
        }
    }

    override fun get(uri: URI, authorization: String?): IHttpResponse {

        // Set the authorization header if authorization string was provided by caller
        // The header is gonna be empty otherwise
        val headers = authorization?.let {

            val newHeaders = HttpHeaders()
            newHeaders.set(AUTHORIZATION, it)
            newHeaders
        } ?: HttpHeaders.EMPTY

        val response = this.restTemplate.exchange(
                uri,
                HttpMethod.GET,
                HttpEntity<String>(headers),
                String::class.java)

        return object : IHttpResponse {

            override val authenticate = response.headers[WWW_AUTHENTICATE]
            override val statusCode = response.statusCode.value()
            override val body = response.body ?: ""
        }
    }
}
