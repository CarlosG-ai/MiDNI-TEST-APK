package es.gob.midni.qrdemo

import com.journeyapps.barcodescanner.CaptureActivity

/**
 * CaptureActivity sin orientación bloqueada.
 * Permite que la línea de escaneo sea siempre horizontal
 * independientemente de si el teléfono está en portrait o landscape.
 */
class AnyOrientationCaptureActivity : CaptureActivity()
