package id.andriawan.cofinance.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.Photos.PHPhotoLibrary
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject

@Composable
actual fun rememberGalleryLauncher(onImageSelected: (String) -> Unit): () -> Unit {
    val callbackRef = rememberUpdatedState(onImageSelected)
    val delegateRef = remember { mutableStateOf<GalleryPickerDelegate?>(null) }

    return remember {
        {
            val configuration = PHPickerConfiguration(PHPhotoLibrary.sharedPhotoLibrary())
            configuration.selectionLimit = 1
            configuration.filter = PHPickerFilter.imagesFilter

            val delegate = GalleryPickerDelegate { identifier ->
                callbackRef.value("ph://$identifier")
                delegateRef.value = null
            }
            delegateRef.value = delegate

            val picker = PHPickerViewController(configuration = configuration)
            picker.delegate = delegate

            val windowScene = UIApplication.sharedApplication.connectedScenes
                .firstOrNull() as? UIWindowScene
            val rootViewController = windowScene?.keyWindow?.rootViewController
            rootViewController?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class GalleryPickerDelegate(
    private val onImageSelected: (String) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
        val identifier = result.assetIdentifier ?: return
        onImageSelected(identifier)
    }
}
