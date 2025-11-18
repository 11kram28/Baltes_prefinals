package ph.edu.comteq.baltes_prefinals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ph.edu.comteq.baltes_prefinals.ui.theme.Baltes_prefinalsTheme



// DATA MODELS
data class Geo(val lat: String, val lng: String)

data class Address(
    val street: String,
    val suite: String,
    val city: String,
    val zipcode: String,
    val geo: Geo
)

data class Company(
    val name: String,
    val catchPhrase: String,
    val bs: String
)

data class User(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    val address: Address,
    val phone: String,
    val website: String,
    val company: Company
)


// API SERVICE
class UserApiService {

    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun fetchUsers(): Result<List<User>> {
        return try {
            val request = Request.Builder()
                .url("https://jsonplaceholder.typicode.com/users")
                .build()

            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP Error: ${response.code}"))
            }

            val body = response.body?.string() ?: return Result.failure(Exception("Empty response"))

            val type = object : TypeToken<List<User>>() {}.type
            val users: List<User> = gson.fromJson(body, type)

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// UI STATE FOR VIEWMODEL
sealed class UserUiState {
    object Loading : UserUiState()
    data class Success(val users: List<User>) : UserUiState()
    data class Error(val message: String) : UserUiState()
}

// VIEWMODEL
class UserViewModel : ViewModel() {

    private val api = UserApiService()

    private val _state = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val state: StateFlow<UserUiState> get() = _state

    init {
        fetchUsers()
    }

    fun fetchUsers() {
        _state.value = UserUiState.Loading
        viewModelScope.launch {
            val result = api.fetchUsers()
            _state.value = result.fold(
                onSuccess = { UserUiState.Success(it) },
                onFailure = { UserUiState.Error(it.localizedMessage ?: "Unknown error") }
            )
        }
    }
}



// MAIN ACTIVITY
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Baltes_prefinalsTheme {
                val userViewModel: UserViewModel = viewModel()
                UserScreen(viewModel = userViewModel)
            }
        }
    }
}

// COMPOSABLE UI
@Composable
fun UserScreen(viewModel: UserViewModel = viewModel()) {
    val uiState by viewModel.state.collectAsState()

    when (uiState) {
        is UserUiState.Loading -> LoadingScreen()
        is UserUiState.Error -> ErrorScreen(
            message = (uiState as UserUiState.Error).message,
            onRetry = { viewModel.fetchUsers() }
        )
        is UserUiState.Success -> UserList(users = (uiState as UserUiState.Success).users)
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Error: $message")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
fun UserList(users: List<User>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(users) { user ->
            UserCard(user)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun UserCard(user: User) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = user.name, style = MaterialTheme.typography.headlineSmall)
            Text(text = "@${user.username}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(user.email)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("City: ${user.address.city}")
        }
    }
}


