package com.example.eac21 // Asegúrate de que coincide con tu paquete

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Modelos de datos
data class User(
    val id: Int,
    val name: String,
    val username: String,
    val email: String
)

data class Post(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)

// Interfaz de API
interface ApiService {
    @GET("users")
    fun getUsers(): Call<List<User>>

    @GET("posts")
    fun getPostsByUserId(@Query("userId") userId: Int): Call<List<Post>>
}

// Configuración de Retrofit
object RetrofitClient {
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}

// Función para obtener la lista de usuarios
fun fetchUsers(onResult: (List<User>) -> Unit) {
    RetrofitClient.instance.getUsers().enqueue(object : Callback<List<User>> {
        override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
            if (response.isSuccessful) {
                onResult(response.body() ?: emptyList())
            } else {
                // Manejar el error aquí
            }
        }

        override fun onFailure(call: Call<List<User>>, t: Throwable) {
            // Manejar el error aquí
        }
    })
}

// Función para obtener los posts de un usuario específico
fun fetchPostsByUserId(userId: Int, onResult: (List<Post>) -> Unit) {
    RetrofitClient.instance.getPostsByUserId(userId).enqueue(object : Callback<List<Post>> {
        override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
            if (response.isSuccessful) {
                onResult(response.body() ?: emptyList())
            } else {
                // Manejar el error aquí
            }
        }

        override fun onFailure(call: Call<List<Post>>, t: Throwable) {
            // Manejar el error aquí
        }
    })
}

// Actividad principal
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Configura la navegación
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "userList") {
                    composable("userList") { UserListScreen(navController) }
                    composable("userPosts/{userId}") { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId")?.toInt() ?: 0
                        UserPostsScreen(userId, navController)
                    }
                }
            }
        }
    }
}

// Pantalla de lista de usuarios
@Composable
fun UserListScreen(navController: NavHostController) {
    val users = remember { mutableStateListOf<User>() }

    // Obtener usuarios
    LaunchedEffect(Unit) {
        fetchUsers { fetchedUsers ->
            users.clear()
            users.addAll(fetchedUsers)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(users) { user ->
            UserListItem(user) {
                // Navegar a la pantalla de posts del usuario seleccionado
                navController.navigate("userPosts/${user.id}")
            }
        }
    }
}

// Composable para un item de usuario
@Composable
fun UserListItem(user: User, onClick: () -> Unit) {
    Text(
        text = user.name,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp) // Añade algo de espacio alrededor del texto
    )
}

// Pantalla de posts de un usuario
@Composable
fun UserPostsScreen(userId: Int, navController: NavHostController) {
    val posts = remember { mutableStateListOf<Post>() }

    // Obtener los posts del usuario
    LaunchedEffect(userId) {
        fetchPostsByUserId(userId) { fetchedPosts ->
            posts.clear()
            posts.addAll(fetchedPosts)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Posts de Usuario ID: $userId", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(posts) { post ->
                PostItem(post)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("userList") }) {
            Text(text = "Volver a la lista de usuarios")
        }
    }
}

// Composable para un item de post
@Composable
fun PostItem(post: Post) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = post.title, style = MaterialTheme.typography.titleMedium)
        Text(text = post.body)
        Spacer(modifier = Modifier.height(8.dp))
    }
}
