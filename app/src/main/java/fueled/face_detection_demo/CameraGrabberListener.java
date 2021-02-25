package fueled.face_detection_demo;

public interface CameraGrabberListener {
    void onCameraInitialized();
    void onCameraError(String errorMsg);
}
