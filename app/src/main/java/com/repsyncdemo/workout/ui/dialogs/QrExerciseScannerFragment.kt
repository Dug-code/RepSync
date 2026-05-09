package com.repsyncdemo.workout.ui.dialogs

/**
 * File overview: Presents a focused dialog or picker flow and returns the selected data to the calling screen.
 */

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.repsyncdemo.workout.data.ExerciseDatabase
import com.repsyncdemo.workout.databinding.FragmentQrExerciseScannerBinding
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QrExerciseScannerFragment : Fragment() {

    private var _binding: FragmentQrExerciseScannerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorkoutViewModel by activityViewModels()

    private var scanner: BarcodeScanner? = null
    private var cameraExecutor: ExecutorService? = null
    private var isProcessingBarcode = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to scan QR codes.", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    // Sets up this screen.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQrExerciseScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Connects views, clicks, and data.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scanner = BarcodeScanning.getClient()
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnCancelScan.setOnClickListener {
            findNavController().popBackStack()
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val binding = _binding ?: return@addListener
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor ?: return@also) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Could not start camera.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (isProcessingBarcode) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessingBarcode = true
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val barcodeScanner = scanner ?: run {
            imageProxy.close()
            isProcessingBarcode = false
            return
        }

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val exerciseName = barcodes
                    .firstNotNullOfOrNull { barcode -> parseExerciseName(barcode) }

                if (exerciseName != null) {
                    deliverExercise(exerciseName)
                }
            }
            .addOnFailureListener {
                _binding?.tvScannerStatus?.text = "Could not read that code."
            }
            .addOnCompleteListener {
                isProcessingBarcode = false
                imageProxy.close()
            }
    }

    private fun parseExerciseName(barcode: Barcode): String? {
        val raw = barcode.rawValue?.trim().orEmpty()
        if (raw.isEmpty()) return null

        val candidate = when {
            raw.startsWith("repsync://exercise/", ignoreCase = true) ->
                raw.substringAfterLast("/")
            raw.startsWith("repsync:exercise:", ignoreCase = true) ->
                raw.substringAfter("repsync:exercise:")
            raw.contains("exercise=", ignoreCase = true) ->
                raw.substringAfter("exercise=").substringBefore("&")
            else -> raw
        }.replace("+", " ").trim()

        return findExerciseName(candidate)
    }

    private fun findExerciseName(candidate: String): String? {
        val decoded = java.net.URLDecoder.decode(candidate, "UTF-8")
        val allExercises = viewModel.allLibraryExercises.value.ifEmpty { ExerciseDatabase.allExercises }
        return allExercises.firstOrNull { exercise ->
            exercise.name.equals(decoded, ignoreCase = true) ||
                exercise.idInData.equals(decoded, ignoreCase = true) ||
                exercise.firebaseId.equals(decoded, ignoreCase = true)
        }?.name
    }

    private fun deliverExercise(exerciseName: String) {
        if (_binding == null) return

        findNavController().previousBackStackEntry
            ?.savedStateHandle
            ?.set("selectedExerciseName", exerciseName)
        Toast.makeText(requireContext(), "$exerciseName added", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    // Clears the view binding.
    override fun onDestroyView() {
        scanner?.close()
        scanner = null
        cameraExecutor?.shutdown()
        cameraExecutor = null
        _binding = null
        super.onDestroyView()
    }
}
