package com.example.androidhttpclient

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.androidhttpclient.ui.theme.AndroidHTTPClientTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import timber.log.Timber

class BackgroundService : Service() {
    private val serviceScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private var coroutineJob = serviceScope.launch { }

    // We are not using binded service, service can only be started and stopped
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        coroutineJob = serviceScope.launch {
            Timber.d("Service start")
            val userId = intent?.getIntExtra("user_id",0)
            val retrofit = Retrofit.Builder()
                .baseUrl("https://dummyjson.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val apiService = retrofit.create(DeleteService::class.java)
            apiService.deleteUser(userId.toString()).enqueue(object : Callback<Unit> {
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    if (response.isSuccessful) {
                        Timber.d("User DELETE successful")
                    } else {
                        Timber.d("User DELETE failed")
                    }
                    stopSelf()
                }
                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    stopSelf()
                }
            })
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineJob.cancel()
        Timber.d("Service Stop")
    }
}


@Serializable
data class User(
    val firstName: String,
    val lastName: String,
    val image: String
)

interface GetService{
    @GET("/users/{endpoint}")
    suspend fun getUsers(@Path("endpoint") endpoint: String): User
}

interface DeleteService {
    @DELETE("/users/{id}")
    fun deleteUser(@Path("id") id: String): Call<Unit> // Unit for successful deletion
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(Timber.treeCount <= 0) {
            Timber.plant(Timber.DebugTree())
        }
        setContent {
            AndroidHTTPClientTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App()
                }
            }
        }
    }
}

@Composable
fun App() {
    val users = remember { mutableStateListOf<User>() }
    Column {
        //FetchButton(users)
        Fetch(users)
        if (users.isNotEmpty()) {
            UserList(users)
        }
    }
}

@Composable
fun UserList(users: SnapshotStateList<User>) {
    val context = LocalContext.current
    val intent = Intent(context, BackgroundService::class.java)
    val coroutineScope = rememberCoroutineScope()
    LazyColumn {
        items(users.size) { item ->
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(5.dp),
            ) {
                Row {
                    AsyncImage(
                        modifier = Modifier
                            .height(50.dp)
                            .width(50.dp),
                        model = users[item].image,
                        contentDescription = "User Image", // For accessibility
                    )
                    Text(
                        text = "${users[item].firstName} ${users[item].lastName}",
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .weight(1.5f)
                    )
                    Button(
                        modifier = Modifier
                            .weight(1f),
                        onClick = {
                            intent.putExtra("user_id", item+1) // Pass the URL to the service
                            context.startService(intent) // Start the service with the intent
                            users.remove(users[item])
                    }) {
                        Text(text = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun Fetch(users: SnapshotStateList<User>) {
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(users) {
        coroutineScope.launch(Dispatchers.IO) {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://dummyjson.com") // Base URL for the API.
                .addConverterFactory(GsonConverterFactory.create()) // Adds Gson converter for JSON parsing.
                .build()
            // Creates an instance of the service interface to call API methods.
            val service = retrofit.create(GetService::class.java)
            var id = 1
            var user: User
            while(true) {
                try {
                    user = service.getUsers(id.toString())
                    users.add(user)
                    id++
                    Timber.d("add user")
                } catch (e: Exception){
                    Timber.d(e.toString())
                    break
                }
            }
        }
    }
}

@Composable
fun FetchButton(users: SnapshotStateList<User>) {
    val coroutineScope = rememberCoroutineScope()
    var isFetchBtEnabled by remember { mutableStateOf(true) }

    Button(
        modifier = Modifier
            .fillMaxWidth(),
        enabled = isFetchBtEnabled,
        onClick = {
            isFetchBtEnabled = false
            coroutineScope.launch(Dispatchers.Default) {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://dummyjson.com") // Base URL for the API.
                    .addConverterFactory(GsonConverterFactory.create()) // Adds Gson converter for JSON parsing.
                    .build()
                // Creates an instance of the service interface to call API methods.
                val service = retrofit.create(GetService::class.java)
                var id = 1
                var user: User
                while(true) {
                    try {
                        user = service.getUsers(id.toString())
                        users.add(user)
                        id++
                        Timber.d("add user")
                    } catch (e: Exception){
                        Timber.d(e.toString())
                        break
                    }
                }
                isFetchBtEnabled = true
            }
        }) {
        Text(text = if(isFetchBtEnabled)"Fetch Users" else "Fetching...")
    }
}