package uk.co.jcox.snake

import org.joml.Vector3f
import org.lwjgl.opengl.*

const val RGB_MAX_VALUE: Float = 255F

class Renderer {

    fun setup() {
        GL.createCapabilities()
        GLUtil.setupDebugMessageCallback()
    }

    private val clearColour: Vector3f = Vector3f(0.0f, 0.0f, 0.0f)

    fun setClearColour(x: Float, y: Float, z: Float) {
        clearColour.x = x
        clearColour.y = y
        clearColour.z = z
        clearColour.div(RGB_MAX_VALUE)
    }


    fun clearScreen() {
        GL11.glClearColor(clearColour.x, clearColour.y, clearColour.z, 1.0f)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
    }


    fun draw(objrep: ObjRepresentable, shader: ShaderProgram, screenX: Int, screenY: Int) {
        GL11.glViewport(0, 0, screenX, screenY)
        shader.bind()
        GL30.glBindVertexArray(objrep.geom.vaoId)
        if (DEBUG_MODE) {
            shader.validateProgram()
        }
        shader.send("diffuseColour", objrep.material.diffuseColour)
        GL11.glDrawElements(GL11.GL_TRIANGLES, objrep.geom.vertexCount, GL11.GL_UNSIGNED_INT, 0)
    }
}