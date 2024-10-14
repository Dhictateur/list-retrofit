package com.example.eac21 // Asegúrate de que coincide con tu paquete

import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.ViewModel
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

// Interfaz del Repositorio
interface UserRepository {
    fun getUsers(onResult: (List<User>) -> Unit)
    fun getPostsByUserId(userId: Int, onResult: (List<Post>) -> Unit)
}

// Implementación del Repositorio
// Implementación del Repositorio
class UserRepositoryImpl : UserRepository {
    private val apiService = RetrofitClient.instance

    override fun getUsers(onResult: (List<User>) -> Unit) {
        apiService.getUsers().enqueue(object : Callback<List<User>> {
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

    override fun getPostsByUserId(userId: Int, onResult: (List<Post>) -> Unit) {
        apiService.getPostsByUserId(userId).enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (response.isSuccessful) {
                    onResult(response.body() ?: emptyList())
                } else {
                    // Manejar el error aquí
                }
            }

            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                Log.e("API Error", "Error fetching posts: ${t.message}")
                // Aquí puedes manejar el error, como mostrar un Toast
            }
        })
    }
}

// ViewModel
class UserViewModel(private val userRepository: UserRepository) : ViewModel() {
    var users by mutableStateOf<List<User>>(emptyList())
    var posts by mutableStateOf<List<Post>>(emptyList())

    fun fetchUsers() {
        userRepository.getUsers { fetchedUsers ->
            users = fetchedUsers
        }
    }

    fun fetchPostsByUserId(userId: Int) {
        userRepository.getPostsByUserId(userId) { fetchedPosts ->
            posts = fetchedPosts
        }
    }
}

// Actividad principal
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Configura la navegación
                val navController = rememberNavController()
                val userRepository = UserRepositoryImpl()
                val userViewModel = UserViewModel(userRepository)

                NavHost(navController = navController, startDestination = "userList") {
                    composable("userList") { UserListScreen(userViewModel, navController) }
                    composable("userPosts/{userId}") { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId")?.toInt() ?: 0
                        UserPostsScreen(userId, userViewModel, navController)
                    }
                }
            }
        }
    }
}

// Pantalla de lista de usuarios
@Composable
fun UserListScreen(userViewModel: UserViewModel, navController: NavHostController) {
    // Obtener usuarios al iniciar la pantalla
    LaunchedEffect(Unit) {
        userViewModel.fetchUsers()
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(userViewModel.users) { user ->
            UserListItem(user) {
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
fun UserPostsScreen(userId: Int, userViewModel: UserViewModel, navController: NavHostController) {
    // Obtener los posts del usuario al iniciar la pantalla
    LaunchedEffect(userId) {
        userViewModel.fetchPostsByUserId(userId)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = { navController.navigate("userList") }) {
            Text(text = "Volver a la lista de usuarios")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Posts de Usuario ID: $userId", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(userViewModel.posts) { post ->
                PostItem(post)
            }
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
