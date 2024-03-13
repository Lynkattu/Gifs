package retrofit

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.Scanner


data class Data(
    val name: String,
    val username: String,
    val email: String
)

@Serializable
data class User( val name: String)

interface PostService {
    @POST("/users")
    fun postData(@Body data:Data): Call<Data>
}

interface DeleteService {
    @DELETE("/users/{id}")
    fun deleteUser(@Path("id") id: String): Call<Unit> // Unit for successful deletion
}

interface GetService{
    @GET("/users/{endpoint}")
    suspend fun getUsers(@Path("endpoint") endpoint: String): User
}

fun post(name: String,username: String,email: String){
    val data = Data(name,username,email)
    val retrofit = Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val apiService = retrofit.create(PostService::class.java)
    apiService.postData(data).enqueue(object : Callback<Data> {
        override fun onResponse(call: Call<Data>, response: Response<Data>) {
            if (response.isSuccessful) {
                println("User POST successful")

            } else {
                println("User POST failed")
            }
        }
        override fun onFailure(call: Call<Data>, t: Throwable) {
            // handle the failure
        }
    })
}

fun delete(id: String) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val apiService = retrofit.create(DeleteService::class.java)
    apiService.deleteUser(id).enqueue(object : Callback<Unit> {
        override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
            if (response.isSuccessful) {
                println("User DELETE successful")
            } else {
                println("User DELETE failed")
            }
        }

        override fun onFailure(call: Call<Unit>, t: Throwable) {
            // Handle network error
        }
    })
}

fun get(strArray: Array<String>){
    val jobs: MutableList<Job> = arrayListOf()
    val users: MutableList<User> = arrayListOf()

    runBlocking{
        val retrofit = Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com") // Base URL for the API.
            .addConverterFactory(GsonConverterFactory.create()) // Adds Gson converter for JSON parsing.
            .build()
        // Creates an instance of the service interface to call API methods.
        val service = retrofit.create(GetService::class.java)
        for(i in strArray){
            val job = launch(Dispatchers.IO) { // launch a new coroutine and continue
                users.add(service.getUsers(i))
            }
            jobs.add(job)
        }
        for(i in 0 until jobs.size){
            jobs[i].join()
        }
    }
    for(i in 0 until jobs.size){
        println(users[i].name)
    }
    println()
}

fun main() {
    val scanner = Scanner(System.`in`)
    while(true) {
        println("1) Add user")
        println("2) Delete user")
        println("3) Display users")
        println("4) Exit")
        when (scanner.nextLine()) {
            "1" -> {
                print("name: ")
                val name = scanner.nextLine()
                print("username: ")
                val username = scanner.nextLine()
                print("email: ")
                val email = scanner.nextLine()
                post(name,username,email)
            }
            "2" -> {
                print("id: ")
                val id = scanner.nextLine()
                delete(id)
            }
            "3" -> {
                print("id's: ")
                val id = scanner.nextLine()
                val strArray = id.split(",").toTypedArray()
                get(strArray)
            }
            //exiting application can take time
            "4" -> return
        }
    }
}
