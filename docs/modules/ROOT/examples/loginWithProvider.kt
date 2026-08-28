import android.support.v7.app.AppCompatActivity
import com.reach5.identity.sdk.core.ReachFive

class MainActivity : AppCompatActivity() {
  // List the providers created on your ReachFive client
  val providers = ReachFive.getProviders()[0]
  // List of ReachFive scope values assigned to the user
  val scope = setOf("openid", "email", "profile", "phone_number", "offline_access", "events", "full_write")

  client.loginWithProvider(providers[0].name, scope, "home", this)
}
