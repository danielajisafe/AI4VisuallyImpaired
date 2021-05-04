package com.journaldev.androidarcoredistancecamera;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.media.Image;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.core.HitResult;
import com.google.android.gms.tasks.*;
import android.content.ContextWrapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.view.PixelCopy;

//import com.google.firebase.FirebaseApp;
//import com.google.firebase.ml.vision.FirebaseVision;
//import com.google.firebase.ml.vision.common.FirebaseVisionImage;

//import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.PredefinedCategory;

import java.util.Objects;
import java.util.List;
import androidx.annotation.NonNull;
import android.graphics.Rect;

public class MainActivity extends AppCompatActivity implements Scene.OnUpdateListener {

    // Live detection and tracking
    ObjectDetectorOptions options =
            new ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                    .enableClassification()  // Optional
                    .build();
    ObjectDetector objectDetector = ObjectDetection.getClient(options);

    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final String TAG = MainActivity.class.getSimpleName();

    private ArFragment arFragment;
    private AnchorNode currentAnchorNode;
    private TextView tvDistance;
    ModelRenderable cubeRenderable;
    private Anchor currentAnchor = null;
    private float count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(getApplicationContext(), "Device not supported", Toast.LENGTH_LONG).show();
        }
//        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);
        initModel();
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);


        tvDistance = findViewById(R.id.tvDistance);
        // Update itself will be called frequently (at a rate ~30fps), so we can simply remove the "setOnTap" listener.
        // and, let it call the desired function.
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onFrame);

        /*
        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (cubeRenderable == null)
                return;

            // Creating Anchor.
            Frame testFrame = arFragment.getArSceneView().getArFrame();
            List<HitResult> randAnchors = testFrame.hitTest(1079, 2279);
            Anchor anchor = randAnchors.get(0).createAnchor();
            Log.d("SOME", "Coordinates " + motionEvent.getX() + motionEvent.getY());
//            Anchor anchor = hitResult.createAnchor();

            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());

            clearAnchor();

            currentAnchor = anchor;
            currentAnchorNode = anchorNode;

            TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
            node.setRenderable(cubeRenderable);
            node.setParent(anchorNode);
            arFragment.getArSceneView().getScene().addOnUpdateListener(this);
            arFragment.getArSceneView().getScene().addChild(anchorNode);
            node.select();

        });

        */
    }

    public boolean checkIsSupportedDeviceOrFinish(final Activity activity) {

        String openGlVersionString =
                ((ActivityManager) Objects.requireNonNull(activity.getSystemService(Context.ACTIVITY_SERVICE)))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    private void initModel() {
        MaterialFactory.makeTransparentWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> {
                            Vector3 vector3 = new Vector3(0.05f, 0.01f, 0.01f);
                            cubeRenderable = ShapeFactory.makeCube(vector3, Vector3.zero(), material);
                            cubeRenderable.setShadowCaster(false);
                            cubeRenderable.setShadowReceiver(false);
                        });
    }

    private void clearAnchor() {
        currentAnchor = null;


        if (currentAnchorNode != null) {
            arFragment.getArSceneView().getScene().removeChild(currentAnchorNode);
            currentAnchorNode.getAnchor().detach();
            currentAnchorNode.setParent(null);
            currentAnchorNode = null;
        }
    }

    private String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,"profile.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }


    private void onFrame(FrameTime frameTime) {
//        Log.d("STATE", "CALLED --" + frameTime.getDeltaSeconds());
        ArSceneView view = arFragment.getArSceneView();
//        Log.d("STATE", "view : " + view.getWidth() + " height" + view.getHeight());
        Frame frame =view.getArFrame();
//        Log.d("STATE", "Get frame. Now try to get image");

        Image image;
        try {
            image = frame.acquireCameraImage();
        }  catch (NotYetAvailableException e){
            e.printStackTrace();
            return;
        }

        Log.d("STATE", "Running object detector!");

        // When running the code below, it crashes without any warning ...
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);

        objectDetector.process(inputImage)
                .addOnSuccessListener(
                        new OnSuccessListener<List<DetectedObject>>() {
                            @Override
                            public void onSuccess(List<DetectedObject> detectedObjects) {
                                Log.d("SOME", "look here " + detectedObjects.size());
                                for (DetectedObject detectedObject: detectedObjects) {
                                    Rect boundingBox = detectedObject.getBoundingBox();
                                    float objx = (float) ((boundingBox.right + boundingBox.left) / 2.0);
                                    float objy = (float) ((boundingBox.bottom + boundingBox.top) / 2.0);
                                    Log.d("STATE", "Object detected X:" + objx + ", Y:" + objy);
                                    //randAnchors = testFrame.hitTest(objx, objy);
                                    //anchor = randAnchors.get(0).createAnchor();
                                    //Integer trackingId = detectedObject.getTrackingId();
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Object detection failed!", e);
                            }
                        });
        /*
        HandlerThread handlerThread = new HandlerThread("Pixel Copier");
        handlerThread.start();
        PixelCopy.request(view, bitmap, copyResult -> {
            if (copyResult == PixelCopy.SUCCESS){
                FirebaseVisionImage fb_image = FirebaseVisionImage.fromBitmap(bitmap);
                Log.d("STATE", "SUCCESSFULLY GET FIREBASE IMAGE");
                FirebaseVision.getInstance().getOnDeviceObjectDetector().processImage(fb_image)
                        .addOnSuccessListener(
                        new OnSuccessListener<List<FirebaseVisionObject>>() {
                            @Override
                            public void onSuccess(List<FirebaseVisionObject> firebaseVisionObjects) {
                                Log.d("DETECTOR", "=====object detected======");
                            }
                        })
                        .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("DETECTOR", "detection failed");
                            }
                        }
                );
            }
            }
        , new Handler(handlerThread.getLooper()));
        */
        // release image resource
        image.close();
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame testFrame = arFragment.getArSceneView().getArFrame();
        Image image;
        Log.d("STATE", "ON UPDATE ACTIVATED");
        try
        {
            image = testFrame.acquireCameraImage();
        }
        catch (NotYetAvailableException e){
            e.printStackTrace();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(image, 0);
        List<HitResult> randAnchors = testFrame.hitTest(500, 500);
        Anchor anchor = randAnchors.get(0).createAnchor();

        objectDetector.process(inputImage)
                .addOnSuccessListener(
                        new OnSuccessListener<List<DetectedObject>>() {
                            @Override
                            public void onSuccess(List<DetectedObject> detectedObjects) {
                                Log.d("SOME", "look here ");
                                for (DetectedObject detectedObject: detectedObjects) {
                                    Rect boundingBox = detectedObject.getBoundingBox();
                                    float objx = (float) ((boundingBox.right + boundingBox.left) / 2.0);
                                    float objy = (float) ((boundingBox.bottom + boundingBox.top) / 2.0);
                                    Log.d("STATE", "Object detected X:" + objx + ", Y:" + objy);
                                    //randAnchors = testFrame.hitTest(objx, objy);
                                    //anchor = randAnchors.get(0).createAnchor();
                                    //Integer trackingId = detectedObject.getTrackingId();
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Object detection failed!", e);
                            }
                        });
        // The list of detected objects contains one item if multiple
        // object detection wasn't enabled.


        Log.d("API123", "onUpdateframe... current anchor node " + (currentAnchorNode == null));


        if (currentAnchorNode != null) {
            Pose objectPose = currentAnchor.getPose();
            Pose cameraPose = testFrame.getCamera().getPose();

            float dx = objectPose.tx() - cameraPose.tx();
            float dy = objectPose.ty() - cameraPose.ty();
            float dz = objectPose.tz() - cameraPose.tz();

//            List<HitResult> randAnchors = frame.hitTest(objectPose.tx(), objectPose.ty());
//            Log.d("SOME", "look here " + (randAnchors.get(0)));

            ///Compute the straight-line distance.
            float distanceMeters = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            tvDistance.setText("Distance from camera: " + distanceMeters + " metres");

            /*float[] distance_vector = currentAnchor.getPose().inverse()
                    .compose(cameraPose).getTranslation();
            float totalDistanceSquared = 0;
            for (int i = 0; i < 3; ++i)
                totalDistanceSquared += distance_vector[i] * distance_vector[i];*/
        }
    }
}
