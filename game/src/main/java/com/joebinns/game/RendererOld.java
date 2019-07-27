package com.joebinns.game;

import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.*;
import com.joebinns.engine.GameItem;
import com.joebinns.engine.Utils;
import com.joebinns.engine.Window;
import com.joebinns.engine.graph.Camera;
import com.joebinns.engine.graph.ShaderProgram;
import com.joebinns.engine.graph.Transformation;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import static org.lwjgl.glfw.GLFW.glfwGetTime;


// CHANGE FILE NAME TO Renderer AND CLASS NAME TO Renderer TO MAKE IT WORK
public class RendererOld {

    /**
     * Field of View in Radians
     */
    private static final float FOV = (float) Math.toRadians(60.0f);

    private static final float Z_NEAR = 0.01f;

    private static final float Z_FAR = 1000.f;

    private final Transformation transformation;

    private ShaderProgram shaderProgram;

    // Compute shader group sizes
    private int workGroupSizeX;
	private int workGroupSizeY;

    public Renderer() {
        transformation = new Transformation();
    }

    public void init(Window window) throws Exception {
        // Create shader
        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader(Utils.loadResource("/shaders/vertex.vs"));
        shaderProgram.createFragmentShader(Utils.loadResource("/shaders/fragment.fs"));
        shaderProgram.createComputeShader(Utils.loadResource("/shaders/raymarching.glsl"));
        shaderProgram.link();
        
        // Create uniforms for modelView and projection matrices and texture
        shaderProgram.createUniform("projectionMatrix");
        shaderProgram.createUniform("modelViewMatrix");
        shaderProgram.createUniform("texture_sampler");

        // Setup Group Sizes for compute shaders
        // TODO: Should this be handled inside the ShaderProgram script...?
        IntBuffer workGroupSize = BufferUtils.createIntBuffer(3);
		glGetProgramiv(shaderProgram.computeShaderId, GL_COMPUTE_WORK_GROUP_SIZE, workGroupSize);
		workGroupSizeX = workGroupSize.get(0);
        workGroupSizeY = workGroupSize.get(1);

        // Create uniforms for compute shaders
        shaderProgram.createUniform("timeValue");
    }

    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void render(Window window, Camera camera, GameItem[] gameItems) {
        clear();
        
        if ( window.isResized() ) {
            glViewport(0, 0, window.getWidth(), window.getHeight());
            window.setResized(false);
        }

        shaderProgram.bind();

        // Set compute shader vars
        shaderProgram.setUniform("timeValue", (float)glfwGetTime());

        /* Bind level 0 of framebuffer texture as writable image in the shader. */
		//glBindImageTexture(0, tex, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);

        /* Compute appropriate invocation dimension. */
		int worksizeX = Utils.nextPowerOfTwo(window.getWidth());
        int worksizeY = Utils.nextPowerOfTwo(window.getHeight());
        
        /* Invoke the compute shader. */
		glDispatchCompute(worksizeX / workGroupSizeX, worksizeY / workGroupSizeY, 1);
        
        // Update projection Matrix
        Matrix4f projectionMatrix = transformation.getProjectionMatrix(FOV, window.getWidth(), window.getHeight(), Z_NEAR, Z_FAR);
        shaderProgram.setUniform("projectionMatrix", projectionMatrix);

        // Update view Matrix
        Matrix4f viewMatrix = transformation.getViewMatrix(camera);
        
        shaderProgram.setUniform("texture_sampler", 0);
        // Render each gameItem
        for(GameItem gameItem : gameItems) {
            // Set model view matrix for this item
            Matrix4f modelViewMatrix = transformation.getModelViewMatrix(gameItem, viewMatrix);
            shaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
            // Render the mes for this game item
            gameItem.getMesh().render();
        }

        shaderProgram.unbind();
    }

    public void cleanup() {
        if (shaderProgram != null) {
            shaderProgram.cleanup();
        }
    }
}
