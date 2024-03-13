package httpclients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsVarArgs
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class User(val name: String)


interface UserService{
    @GET("/users/{endpoint}")
    suspend fun fetchRetrofit(@Path("endpoint") endpoint: String): User
}

fun fetchOkhttp(id: String): String{
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://jsonplaceholder.typicode.com/users/$id")
        .build()
    val response = client.newCall(request).execute()

        val jsonNullable: String? = response.body?.string()
        val json = jsonNullable ?: """{name: "null"}"""
        response.close()
        return json
}

suspend fun fetchKtor(id: String): User {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    client.use {
        return it.get("https://jsonplaceholder.typicode.com/users/$id").body()
    }
}

fun main(args: Array<String>) {
    if(args.isNotEmpty()) {
        if(args[0] == "ktor")
            runBlocking {
            for(i in 1 until args.size) {
                launch(Dispatchers.IO) {
                    val result: User = fetchKtor(args[i])
                    println(result.name)
                }
            }
        }
        else if(args[0] == "okhttp") {
            runBlocking{
                for(i in 1 until args.size) {
                    launch(Dispatchers.IO){
                        val result: String = fetchOkhttp(args[i])
                        val decoded: User = Json{ ignoreUnknownKeys = true }.decodeFromString<User>(result)
                        println(decoded.name)
                    }
                }
            }
        }
        else if(args[0] == "retrofit") {
            runBlocking {
                // Initializes Retrofit with base URL and JSON converter for Kotlin serialization.
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://jsonplaceholder.typicode.com") // Base URL for the API.
                    .addConverterFactory(GsonConverterFactory.create()) // Adds Gson converter for JSON parsing.
                    .build()

                // Creates an instance of the service interface to call API methods.
                val service = retrofit.create(UserService::class.java)
                for(i in 1 until args.size) {
                    // Launches a coroutine for network call on the I/O dispatcher for offloading blocking IO tasks.
                    launch(Dispatchers.IO) {
                        //specific endpoint in url
                        val endpoint = args[i]
                        // Makes a network call to fetch a joke and stores the result.
                        val user : User = service.fetchRetrofit(endpoint)
                        println(user.name)
                    }
                }
            }
        }
    }
}
