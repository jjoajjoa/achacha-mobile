import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.nio.ByteBuffer;

private Interpreter tflite;

private void loadModel() {
    try {
        // 모델 파일 로드
        Interpreter.Options options = new Interpreter.Options();
        tflite = new Interpreter(FileUtil.loadMappedFile(this, "model.tflite"), options);
    } catch (Exception e) {
        Log.e("TensorFlow", "모델 로드 실패", e);
    }
}

private void processImage(ImageProxy image) {
    // 이미지 데이터를 TensorFlow Lite 모델에 맞게 변환
    Bitmap bitmap = imageProxyToBitmap(image);

    // 모델에 이미지 전달
    float[][] result = new float[1][1]; // 결과 배열 크기는 모델에 따라 다를 수 있음
    tflite.run(bitmap, result); // 모델 실행

    // 결과 처리 (눈 감김 여부 판단 등)
    if (result[0][0] > 0.5) {
        // 눈 감김 상태
    }
}

private Bitmap imageProxyToBitmap(ImageProxy image) {
    // ImageProxy를 Bitmap으로 변환하는 코드 작성
    Image imagePlane = image.getPlanes()[0];
    ByteBuffer buffer = imagePlane.getBuffer();
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);

    return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
}
