import SwiftUI
import Shared

/// Lo único de Swift que tiene la app: arrancar Kotlin y mostrar Compose.
///
/// El arranque va en el AppDelegate y no en la vista: tiene que correr una sola vez y antes que
/// cualquier pantalla.
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        MainViewControllerKt.iniciar()
        return true
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView().ignoresSafeArea(.all)
        }
    }
}
