package midni.test

import com.journeyapps.barcodescanner.CaptureActivity

/**
 * CaptureActivity sin orientaciÃ³n bloqueada.
 * Permite que la lÃ­nea de escaneo sea siempre horizontal
 * independientemente de si el telÃ©fono estÃ¡ en portrait o landscape.
 */
class AnyOrientationCaptureActivity : CaptureActivity()

