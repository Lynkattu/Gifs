package com.example.giphy

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.example.giphy.ui.theme.GiphyTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import timber.log.Timber

data class GiphyResponse(val data: List<GiphyData>)

//Giphy data structure
data class GiphyData(val id: String, val images: GiphyImage, val url: String)
data class GiphyImage(val original: GiphyOriginal )
data class GiphyOriginal(val url: String)

interface GiphyApi {
    @GET("v1/gifs/search")
    suspend fun searchGifs(
        @Query("api_key") apiKey: String,
        @Query("q") query: String,
        @Query("limit") limit: Int
    ): GiphyResponse
}

interface GiphyTrendApi {
    @GET("v1/gifs/trending")
    suspend fun searchTrendingGifs(
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int
    ): GiphyResponse
}

class GiphyClient {
    private val apiKey = "KQ4SwmGDYhtNMuL4DrcQx73kMQBg4z51"
    private val baseUrl = "https://api.giphy.com/"
    private val limit = 10
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val giphyApi = retrofit.create(GiphyApi::class.java)
    private val giphyTrendApi = retrofit.create(GiphyTrendApi::class.java)

    suspend fun searchGifs(query: String): GiphyResponse {
        return giphyApi.searchGifs(apiKey, query, limit)
    }

    suspend fun trendGifs(): GiphyResponse {
        return giphyTrendApi.searchTrendingGifs(apiKey,limit)
    }
}

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(Timber.treeCount <= 0) {
            Timber.plant(Timber.DebugTree())
        }
        setContent {
            GiphyTheme {
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

//add searched giphyData to gif state
suspend fun searchGiphy(gif: SnapshotStateList<GiphyData>, query: String) {
    gif.clear()
    val giphyClient = GiphyClient()
    val g: GiphyResponse = giphyClient.searchGifs(query)
    g.data.forEach{
        gif.add(it)
    }
}
//add trending giphyData to gif state
suspend fun trendGiphy(gif: SnapshotStateList<GiphyData>) {
    gif.clear()
    val giphyClient = GiphyClient()
    val g: GiphyResponse = giphyClient.trendGifs()
    g.data.forEach{
        gif.add(it)
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun App() {
    val gif = remember { mutableStateListOf<GiphyData>() }
    val tabTitles = listOf("Search", "Trending")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column {
        // Tab row
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = title) }
                )
            }
        }
        // Content based on selected tab
        when (selectedTabIndex) {
            0 -> SearchTabContent(gif)
            1 -> TrendTabContent()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun GifImage(url: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .decoderFactory(ImageDecoderDecoder.Factory())
            .build(),
        contentDescription = null,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .padding(18.dp, 8.dp)
            .height(200.dp)
            .fillMaxWidth(),
        contentScale = ContentScale.Crop
    )
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun LazyColumnForGifs(gif: SnapshotStateList<GiphyData>) {
    LazyColumn {
        items(gif.size) { i ->
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center

            ) {
                GifImage(gif[i].images.original.url)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun SearchTabContent(gif: SnapshotStateList<GiphyData>) {
    var search by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    TextField(
        value = search,
        onValueChange = { newText -> search = newText },
        label = { Text("Search gifs") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    )
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        onClick = {
            coroutineScope.launch {
                searchGiphy(gif,search)
            }
        }) {
        Text(text = "Search Gifs")
    }
    if(gif.size > 0) {
        LazyColumnForGifs(gif)
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun TrendTabContent(){
    val gif = remember { mutableStateListOf<GiphyData>() }
    LaunchedEffect(gif){
        trendGiphy(gif)
    }
    if(gif.size > 0) {
        LazyColumnForGifs(gif)
    }
}