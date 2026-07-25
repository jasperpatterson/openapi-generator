package org.openapitools.client.apis

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Call
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import org.openapitools.client.models.AnyOfBooleanOrDoubleOrString
import org.openapitools.client.models.AnyOfUserOrPet
import org.openapitools.client.models.AnyOfUserOrPetOrArrayString
import org.openapitools.client.models.BooleanOrDoubleOrString
import org.openapitools.client.models.BooleanOrLong
import org.openapitools.client.models.FloatOrInt
import org.openapitools.client.models.NumberOrString
import org.openapitools.client.models.StringOrLong
import org.openapitools.client.models.UserOrPet
import org.openapitools.client.models.UserOrPetOrArrayString

public interface TestApi {
    /**
     * GET v1/test/anyOf
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [Call]<[AnyOfUserOrPet]>
     */
    @GET("v1/test/anyOf")
    public fun getAnyOf(): Call<AnyOfUserOrPet>

    /**
     * GET v1/test/anyOfArray
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [Call]<[AnyOfUserOrPetOrArrayString]>
     */
    @GET("v1/test/anyOfArray")
    public fun getAnyOfArray(): Call<AnyOfUserOrPetOrArrayString>

    /**
     * GET v1/test/anyOfDoublePrimitive
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [Call]<[AnyOfBooleanOrDoubleOrString]>
     */
    @GET("v1/test/anyOfDoublePrimitive")
    public fun getAnyOfDoublePrimitive(): Call<AnyOfBooleanOrDoubleOrString>

    /**
     * GET v1/test/oneOf
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [Call]<[UserOrPet]>
     */
    @GET("v1/test/oneOf")
    public fun getOneOf(): Call<UserOrPet>

    /**
     * GET v1/test/oneOfArray
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [Call]<[UserOrPetOrArrayString]>
     */
    @GET("v1/test/oneOfArray")
    public fun getOneOfArray(): Call<UserOrPetOrArrayString>

    /**
     * GET v1/test/oneOfBooleanPrimitive
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [Call]<[BooleanOrLong]>
     */
    @GET("v1/test/oneOfBooleanPrimitive")
    public fun getOneOfBooleanPrimitive(): Call<BooleanOrLong>

    /**
     * GET v1/test/oneOfDoublePrimitive
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [Call]<[BooleanOrDoubleOrString]>
     */
    @GET("v1/test/oneOfDoublePrimitive")
    public fun getOneOfDoublePrimitive(): Call<BooleanOrDoubleOrString>

    /**
     * GET v1/test/oneOfFloatIntPrimitive
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [Call]<[FloatOrInt]>
     */
    @GET("v1/test/oneOfFloatIntPrimitive")
    public fun getOneOfFloatIntPrimitive(): Call<FloatOrInt>

    /**
     * GET v1/test/oneOfNumberPrimitive
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [Call]<[NumberOrString]>
     */
    @GET("v1/test/oneOfNumberPrimitive")
    public fun getOneOfNumberPrimitive(): Call<NumberOrString>

    /**
     * GET v1/test/oneOfPrimitive
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [Call]<[StringOrLong]>
     */
    @GET("v1/test/oneOfPrimitive")
    public fun getOneOfPrimitive(): Call<StringOrLong>

}
