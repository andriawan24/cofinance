import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import id.andriawan.cofinance.pages.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Cofinance"
    ) {
        App()
    }
}
