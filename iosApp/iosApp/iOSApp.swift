import SwiftUI
import Shared

/// Lo único de Swift que tiene la app: arrancar Kotlin y mostrar Compose.
///
/// El arranque va en el AppDelegate y no en la vista porque iOS relanza la app en segundo plano
/// cuando se entra a una región vigilada, sin mostrar ninguna pantalla. Si el `CLLocationManager`
/// se creara con la UI, el evento llegaría y nadie lo atendería (D11 de la 006).
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        PilotoKt.iniciarPiloto()
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
