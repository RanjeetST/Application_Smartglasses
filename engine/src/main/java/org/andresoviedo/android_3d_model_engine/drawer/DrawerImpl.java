package org.andresoviedo.android_3d_model_engine.drawer;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import org.andresoviedo.android_3d_model_engine.model.AnimatedModel;
import org.andresoviedo.android_3d_model_engine.model.Object3D;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.util.android.GLUtil;
import org.andresoviedo.util.math.Quaternion;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Draw using single color, texture and skeleton and no light
 *
 * @author andresoviedo
 */
class DrawerImpl implements Object3D {

    private final static int COORDS_PER_VERTEX = 3;
    private final static int VERTEX_STRIDE = COORDS_PER_VERTEX * 4; // 4 bytes per
    private final static float[] DEFAULT_COLOR = {1.0f, 1.0f, 1.0f, 1.0f};
    private final static float[] NO_COLOR_MASK = {1.0f, 1.0f, 1.0f, 1.0f};

    // specification
    private final String id;
    private final Set<String> features;

    // opengl program
    private final int mProgram;

    // temporary variables
    private final float[] mMatrix = new float[16];
    private final float[] mvMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // animation data
    // put 0 to draw progressively, -1 to draw at once
    private long counter = -1;
    private double shift = -1d;
    public Quaternion quaternion;

    // does the device support drawElements for GL_UNSIGNED_INT or not?
    private boolean drawUsingUnsignedInt = true;

    private final SparseArray<String> cache1 = new SparseArray<>();

    public static DrawerImpl getInstance(String id, String vertexShaderCode, String fragmentShaderCode) {
        Set<String> shaderFeatures = new HashSet<>();
        testShaderFeature(shaderFeatures, vertexShaderCode, "a_Position");
        testShaderFeature(shaderFeatures, vertexShaderCode, "u_MVMatrix");
        testShaderFeature(shaderFeatures, vertexShaderCode, "a_Normal");
        testShaderFeature(shaderFeatures, vertexShaderCode, "a_Color");
        testShaderFeature(shaderFeatures, vertexShaderCode, "a_TexCoordinate");
        testShaderFeature(shaderFeatures, vertexShaderCode, "a_EmissiveTexCoordinate");
        testShaderFeature(shaderFeatures, vertexShaderCode, "u_LightPos");
        testShaderFeature(shaderFeatures, vertexShaderCode, "u_MVMatrix");
        testShaderFeature(shaderFeatures, vertexShaderCode, "in_jointIndices");
        testShaderFeature(shaderFeatures, vertexShaderCode, "in_weights");

        testShaderFeature(shaderFeatures, vertexShaderCode, "uRotationMatrix");
        return new DrawerImpl(id, vertexShaderCode, fragmentShaderCode, shaderFeatures);
    }

    private static void testShaderFeature(Set<String> outputFeatures, String shaderCode, String feature) {
        if (shaderCode.contains(feature)) {
            outputFeatures.add(feature);
        }
    }

    private DrawerImpl(String id, String vertexShaderCode, String fragmentShaderCode, Set<String> features) {

        this.id = id;
        this.features = features;
        Log.i("Object3DImpl2", "Compiling 3D Drawer... " + id);

        // load shaders
        int vertexShader = GLUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = GLUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        // compile program
        mProgram = GLUtil.createAndLinkProgram(vertexShader, fragmentShader, features.toArray(new String[features.size()]));
        Log.i("Object3DImpl2", "Compiled 3D Drawer (" + id + ") with id " + mProgram);
    }

    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int textureId, float[] lightPos) {
        this.draw(obj, pMatrix, vMatrix, obj.getDrawMode(), obj.getDrawSize(), textureId, lightPos, null);
    }

    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int textureId, float[] lightPos, float[] colorMask) {
        this.draw(obj, pMatrix, vMatrix, obj.getDrawMode(), obj.getDrawSize(), textureId, lightPos, colorMask);
    }

    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int drawMode, int drawSize, int textureId,
                     float[] lightPos) {
        this.draw(obj, pMatrix, vMatrix, drawMode, drawSize, textureId, lightPos, null);
    }

    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int drawMode, int drawSize, int textureId,
                     float[] lightPos, float[] colorMask) {


        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        float[] mMatrix = getMMatrix(obj);
        float[] mvMatrix = getMvMatrix(mMatrix, vMatrix);
        float[] mvpMatrix = getMvpMatrix(mvMatrix, pMatrix);
/*
		//test add
		Quaternion rotationQuaternion = new Quaternion(0.12f,0.3f,0.5f,3.0f);
		float[] rotationMatrix = new float[16];
		rotationQuaternion.toRotationMatrix(rotationMatrix);

        int rotationMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uRotationMatrix");
        GLES20.glUniformMatrix4fv(rotationMatrixHandle, 1, false, rotationMatrix, 0);

*/

        setMvpMatrix(mvpMatrix);

        int mPositionHandle = setPosition(obj);

        int mNormalHandle = -1;
        if (supportsNormals()) {
            mNormalHandle = setNormals(obj);
        }

        int mColorHandle = -1;
        if (supportsColors()) {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);
            setColor(obj);
            mColorHandle = setColors(obj);
            GLES20.glDisable(GLES20.GL_BLEND);
        } else {
            setColor(obj);
        }
        setColorMask(colorMask);

        // TODO: refactor code here for not using both texture and emissiveTexture
        int mTextureHandle = -1;
        if (textureId != -1 && supportsTextures()) {
            mTextureHandle = setTexture(obj, textureId);
        }


        int mEmissiveTextureHandle = -1;
        if (obj.getEmissiveTextureHandle() != -1 && supportsEmissiveTexture()){
            mEmissiveTextureHandle = setEmissiveTexture(obj);
        }

        // light rendering needs mv matrix
        if (supportsMvMatrix()) {
            setMvMatrix(mvMatrix);
        }

        // TODO: remove this null check
        if (lightPos != null && supportsLighting()) {
            setLightPos(lightPos);
        }

        // joint transformation for animated model
        int in_weightsHandle = -1;
        int in_jointIndicesHandle = -1;
        if (supportsJoints() && obj instanceof AnimatedModel) {
            in_weightsHandle = setWeights((AnimatedModel) obj);
            in_jointIndicesHandle = setJoints((AnimatedModel) obj);
            setJointTransforms((AnimatedModel) obj);
        }

        // draw mesh
		
        drawShape(obj, drawMode, drawSize);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);

        if (mColorHandle != -1) {
            GLES20.glDisableVertexAttribArray(mColorHandle);
        }

        if (mNormalHandle != -1) {
            GLES20.glDisableVertexAttribArray(mNormalHandle);
        }

        // Disable vertex array
        if (mTextureHandle != -1) {
            GLES20.glDisableVertexAttribArray(mTextureHandle);
        }


        if (mEmissiveTextureHandle != -1) {
            GLES20.glDisableVertexAttribArray(mEmissiveTextureHandle);
        }

        if (in_weightsHandle != -1) {
            GLES20.glDisableVertexAttribArray(in_weightsHandle);
            GLES20.glDisableVertexAttribArray(in_jointIndicesHandle);
        }

        if (!obj.getIsDoubleSided()){
            GLES20.glDisable(GLES20.GL_CULL_FACE);
        }

    }

    @Override
    public void setQuaternion(Quaternion quaternion) {
        this.quaternion = quaternion;
    }

    private float[] getMMatrix(Object3DData obj) {

        // calculate object transformation
        Matrix.setIdentityM(mMatrix, 0);
        if (quaternion !=null) {
            float[] rotationMatrix = new float[16];
            quaternion.toRotationMatrix(rotationMatrix);
            Matrix.multiplyMM(mMatrix, 0, rotationMatrix, 0, mMatrix, 0);
        } else if (obj.getRotation() != null) {
            Matrix.rotateM(mMatrix, 0, obj.getRotation()[0], 1f, 0f, 0f);
            Matrix.rotateM(mMatrix, 0, obj.getRotation()[1], 0, 1f, 0f);
            Matrix.rotateM(mMatrix, 0, obj.getRotationZ(), 0, 0, 1f);
        }
        if (obj.getScale() != null) {
            Matrix.scaleM(mMatrix, 0, obj.getScaleX(), obj.getScaleY(), obj.getScaleZ());
        }
        if (obj.getPosition() != null) {
            Matrix.translateM(mMatrix, 0, obj.getPositionX(), obj.getPositionY(), obj.getPositionZ());
        }
        return mMatrix;
    }

    private float[] getMvMatrix(float[] mMatrix, float[] vMatrix) {
        Matrix.multiplyMM(mvMatrix, 0, vMatrix, 0, mMatrix, 0);
        return mvMatrix;
    }

    private float[] getMvpMatrix(float[] mvMatrix, float[] pMatrix) {
        Matrix.multiplyMM(mvpMatrix, 0, pMatrix, 0, mvMatrix, 0);
        return mvpMatrix;
    }

    private void setMvpMatrix(float[] mvpMatrix) {

        // get handle to shape's transformation matrix
        int mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
        GLUtil.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLUtil.checkGlError("glUniformMatrix4fv");
    }

    private boolean supportsColors() {
        return features.contains("a_Color");
    }

    private void setColor(Object3DData obj) {

        // get handle to fragment shader's vColor member
        int mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLUtil.checkGlError("glGetUniformLocation");

        // Set color for drawing the triangle
        float[] color = obj.getColor() != null ? obj.getColor() : DEFAULT_COLOR;
//        Log.d("SetColor", obj.getId() + ": " + color[0] + " " + color[1] + " " + color[2] + " " + color[3] + " ");
//        float[] color = {1f, 0f, 0f, 1f};

        GLES20.glUniform4fv(mColorHandle, 1, color, 0);
        GLUtil.checkGlError("glUniform4fv");
    }

    private int setColors(Object3DData obj) {

        // get handle to fragment shader's vColor member
        int mColorHandle = GLES20.glGetAttribLocation(mProgram, "a_Color");
        GLUtil.checkGlError("glGetAttribLocation");

        // Pass in the color information
        GLES20.glEnableVertexAttribArray(mColorHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");

        obj.getVertexColorsArrayBuffer().position(0);
        GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, obj.getVertexColorsArrayBuffer());
        GLUtil.checkGlError("glVertexAttribPointer");

        return mColorHandle;
    }

    private int setPosition(Object3DData obj) {

        // get handle to vertex shader's a_Position member
        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
        GLUtil.checkGlError("glGetAttribLocation");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");

        FloatBuffer vertexBuffer = obj.getVertexArrayBuffer() != null ? obj.getVertexArrayBuffer()
                : obj.getVertexBuffer();
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE,
                vertexBuffer);
        GLUtil.checkGlError("glVertexAttribPointer");

        return mPositionHandle;
    }

    private boolean supportsNormals() {
        return features.contains("a_Normal");
    }

    private int setNormals(Object3DData obj) {
        int mNormalHandle = GLES20.glGetAttribLocation(mProgram, "a_Normal");
        GLUtil.checkGlError("glGetAttribLocation");

        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");

        // Pass in the normal information
        FloatBuffer buffer = obj.getVertexNormalsArrayBuffer() != null ? obj.getVertexNormalsArrayBuffer() : obj.getNormals();
        buffer.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, 3, GLES20.GL_FLOAT, false, 0, buffer);

        return mNormalHandle;
    }

    private boolean supportsLighting() {
        return features.contains("u_LightPos") && features.contains("u_MVMatrix");
    }

    private void setLightPos(float[] lightPosInEyeSpace) {
        int mLightPosHandle = GLES20.glGetUniformLocation(mProgram, "u_LightPos");
        // Pass in the light position in eye space.
        GLES20.glUniform3f(mLightPosHandle, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);
    }

    private boolean supportsMvMatrix() {
        return features.contains("u_MVMatrix");
    }

    private void setMvMatrix(float[] mvMatrix) {
        int mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVMatrix");
        GLUtil.checkGlError("glGetUniformLocation");

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvMatrix, 0);
        GLUtil.checkGlError("glUniformMatrix4fv");
    }

    private boolean supportsTextures() {
        return features.contains("a_TexCoordinate");
    }

    private boolean supportsEmissiveTexture() {
        return features.contains("a_EmissiveTexCoordinate");
    }

    private void setColorMask(float[] colorMask) {
        int vColorMaskHandle = GLES20.glGetUniformLocation(mProgram, "vColorMask");
        GLUtil.checkGlError("glGetUniformLocation");

        float[] color = colorMask != null ? colorMask : NO_COLOR_MASK;
        GLES20.glUniform4fv(vColorMaskHandle, 1, color, 0);
        GLUtil.checkGlError("glUniform4fv");
    }

    // TODO: refactor to remove duplicate code
    private int setEmissiveTexture(Object3DData obj) {
        // TODO: add emissive texture
        int mEmissiveTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_EmissiveTexture");
        GLUtil.checkGlError("glGetUniformLocation");

        // Set the active texture unit to texture unit 1.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLUtil.checkGlError("glActiveTexture");

        if (!obj.getIsDoubleSided()){
            GLES20.glCullFace(GLES20.GL_BACK);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
        }

        // Bind to the texture in OpenGL
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, obj.getEmissiveTextureHandle());
        GLUtil.checkGlError("glBindTexture");

        // set sampling and filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, obj.getEmissiveTextureMinFilter());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, obj.getEmissiveTextureMagFilter());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, obj.getEmissiveTextureWrapS());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, obj.getEmissiveTextureWrapT());

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mEmissiveTextureUniformHandle, 1);
        GLUtil.checkGlError("glUniform1i");

        int mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_EmissiveTexCoordinate");
        GLUtil.checkGlError("glGetAttribLocation");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");

        // Prepare the triangle coordinate data
        obj.getEmissiveTextureCoordsArrayBuffer().position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0,
                obj.getEmissiveTextureCoordsArrayBuffer());
        GLUtil.checkGlError("glVertexAttribPointer");

        return mTextureCoordinateHandle;
    }

    private int setTexture(Object3DData obj, int textureId) {
        // TODO: add emissive texture
        int mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture");
        GLUtil.checkGlError("glGetUniformLocation");

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLUtil.checkGlError("glActiveTexture");

        if (!obj.getIsDoubleSided()){
            GLES20.glCullFace(GLES20.GL_BACK);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
        }

        // Bind to the texture in OpenGL
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLUtil.checkGlError("glBindTexture");

        // set sampling and filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, obj.getTextureMinFilter());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, obj.getTextureMagFilter());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, obj.getTextureWrapS());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, obj.getTextureWrapT());

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);
        GLUtil.checkGlError("glUniform1i");

        int mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");
        GLUtil.checkGlError("glGetAttribLocation");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");

        // Prepare the triangle coordinate data
        obj.getTextureCoordsArrayBuffer().position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0,
                obj.getTextureCoordsArrayBuffer());
        GLUtil.checkGlError("glVertexAttribPointer");

        return mTextureCoordinateHandle;
    }

    private boolean supportsJoints() {
        return features.contains("in_jointIndices") && features.contains("in_weights");
    }

    private int setWeights(AnimatedModel animatedModel) {
        int in_weightsHandle = GLES20.glGetAttribLocation(mProgram, "in_weights");
        GLUtil.checkGlError("glGetAttribLocation");
        GLES20.glEnableVertexAttribArray(in_weightsHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");
        animatedModel.getVertexWeights().position(0);
        GLES20.glVertexAttribPointer(in_weightsHandle, 3, GLES20.GL_FLOAT, false, 0, animatedModel.getVertexWeights());
        return in_weightsHandle;
    }

    private int setJoints(AnimatedModel animatedModel) {
        int in_jointIndicesHandle = GLES20.glGetAttribLocation(mProgram, "in_jointIndices");
        GLUtil.checkGlError("glGetAttribLocation");
        GLES20.glEnableVertexAttribArray(in_jointIndicesHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");
        animatedModel.getJointIds().position(0);
        GLES20.glVertexAttribPointer(in_jointIndicesHandle, 3, GLES20.GL_FLOAT, false, 0, animatedModel.getJointIds());
        GLUtil.checkGlError("glVertexAttribPointer");
        return in_jointIndicesHandle;
    }

    private void setJointTransforms(AnimatedModel animatedModel) {
        float[][] jointTransformsArray = animatedModel.getJointTransforms();
        // get handle to fragment shader's vColor member

        // TODO: optimize this (memory allocation)
        for (int i = 0; i < jointTransformsArray.length; i++) {
            float[] jointTransform = jointTransformsArray[i];
            // Log.v("DrawerImpl","jointTransform: "+ Arrays.toString(jointTransform));
            String jointTransformHandleName = cache1.get(i);
            if (jointTransformHandleName == null) {
                jointTransformHandleName = "jointTransforms[" + i + "]";
                cache1.put(i, jointTransformHandleName);
            }
            int jointTransformsHandle = GLES20.glGetUniformLocation(mProgram, jointTransformHandleName);
            GLUtil.checkGlError("glGetUniformLocation");
            GLES20.glUniformMatrix4fv(jointTransformsHandle, 1, false, jointTransform, 0);
            //handles.add(jointTransformsHandle);
        }
    }

    private void drawShape(Object3DData obj, int drawMode, int drawSize) {
        FloatBuffer vertexBuffer = obj.getVertexArrayBuffer() != null ? obj.getVertexArrayBuffer()
                : obj.getVertexBuffer();
        vertexBuffer.position(0);
        List<int[]> drawModeList = obj.getDrawModeList();

        Buffer drawOrderBuffer = obj.getDrawOrderBuffer();
        int drawBufferType = obj.getDrawOrderBufferType();


        if (!drawUsingUnsignedInt) {
            drawOrderBuffer = obj.getDrawOrderAsShort();
            drawBufferType = GLES20.GL_UNSIGNED_SHORT;
        }


        if (obj.isDrawUsingArrays()) {
            drawOrderBuffer = null;
        }

        if (drawModeList != null) {
            if (drawOrderBuffer == null) {
                // Log.v(obj.getId(), "Drawing single polygons using arrays...");
                for (int j = 0; j < drawModeList.size(); j++) {
                    int[] polygon = drawModeList.get(j);
                    int drawModePolygon = polygon[0];
                    int vertexPos = polygon[1];
                    int drawSizePolygon = polygon[2];
                    if (drawMode == GLES20.GL_LINE_LOOP && polygon[2] > 3) {
                        // is this wireframe?
                        // Log.v("Object3DImpl","Drawing wireframe for '" + obj.getId() + "' (" + drawSizePolygon + ")...");
                        for (int i = 0; i < polygon[2] - 2; i++) {
                            // Log.v("Object3DImpl","Drawing wireframe triangle '" + i + "' for '" + obj.getId() + "'...");
                            GLES20.glDrawArrays(drawMode, polygon[1] + i, 3);
                        }
                    } else {
                        GLES20.glDrawArrays(drawMode, polygon[1], polygon[2]);
                    }
                }
            } else {
                // Log.d(obj.getId(),"Drawing single polygons using elements...");
                for (int i = 0; i < drawModeList.size(); i++) {
                    int[] drawPart = drawModeList.get(i);
                    int drawModePolygon = drawPart[0];
                    int vertexPos = drawPart[1];
                    int drawSizePolygon = drawPart[2];
                    drawOrderBuffer.position(vertexPos);
                    GLES20.glDrawElements(drawModePolygon, drawSizePolygon, drawBufferType, drawOrderBuffer);
                    if (drawUsingUnsignedInt && GLUtil.checkGlError("glDrawElements")) {
                        drawUsingUnsignedInt = false;
                    }
                }
            }
        } else {
            if (drawOrderBuffer != null) {
                if (drawSize <= 0) {
                    // String mode = drawMode == GLES20.GL_POINTS ? "Points" : drawMode == GLES20.GL_LINES? "Lines": "Triangles?";
                    // Log.v(obj.getId(),"Drawing all elements with mode '"+drawMode+"'...");
                    drawOrderBuffer.position(0);
                    GLES20.glDrawElements(drawMode, drawOrderBuffer.capacity(), drawBufferType,
                            drawOrderBuffer);
                    if (drawUsingUnsignedInt && GLUtil.checkGlError("glDrawElements")) {
                        drawUsingUnsignedInt = false;
                    }
                } else {
                    //Log.d(obj.getId(),"Drawing single elements of size '"+drawSize+"'...");
                    for (int i = 0; i < drawOrderBuffer.capacity(); i += drawSize) {
                        drawOrderBuffer.position(i);
                        GLES20.glDrawElements(drawMode, drawSize, drawBufferType, drawOrderBuffer);
                    }
                    if (drawUsingUnsignedInt && GLUtil.checkGlError("glDrawElements")) {
                        drawUsingUnsignedInt = false;
                    }
                }
            } else {
                if (drawSize <= 0) {
                    int drawCount = vertexBuffer.capacity() / COORDS_PER_VERTEX;

                    // if we want to animate, initialize counter=0 at variable declaration
                    if (this.shift >= 0) {
                        double rotation = ((SystemClock.uptimeMillis() % 10000) / 10000f) * (Math.PI * 2);

                        if (this.shift == 0d) {
                            this.shift = rotation;
                        }
                        drawCount = (int) ((Math.sin(rotation - this.shift + Math.PI / 2 * 3) + 1) / 2f * drawCount);
                    }
                    // Log.d(obj.getId(),"Drawing all triangles using arrays... counter("+drawCount+")");
                    GLES20.glDrawArrays(drawMode, 0, drawCount);
                } else {
                    //Log.d(obj.getId(),"Drawing single triangles using arrays...");
                    for (int i = 0; i < vertexBuffer.capacity() / COORDS_PER_VERTEX; i += drawSize) {
                        GLES20.glDrawArrays(drawMode, i, drawSize);
                    }
                }
            }
        }
    }
}
