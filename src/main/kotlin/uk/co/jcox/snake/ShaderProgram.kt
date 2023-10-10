package uk.co.jcox.snake

import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20
import java.lang.RuntimeException


class ShaderProgram (vararg shaderInfos: ShaderInfo) : BindableState {

    private val shaderIds: MutableList<Int> = mutableListOf()
    private val programId: Int = GL20.glCreateProgram()

    init {

        if (shaderInfos.isEmpty()) {
            throw RuntimeException("No shaders found for compilation")
        }

        for (info in shaderInfos) {
            val shaderId: Int = GL20.glCreateShader(info.shaderType.glObjectType)
            GL20.glShaderSource(shaderId, info.shaderSource)
            GL20.glCompileShader(shaderId)
            GL20.glAttachShader(programId, shaderId)
            shaderIds.add(shaderId)
        }

        GL20.glLinkProgram(programId)

        if (getProgramInfoLog().isNotEmpty()) {
            throw RuntimeException("Shader Compilation failed: " + getProgramInfoLog());
        }

        bind()

        validateProgram()
    }

    fun getProgramInfoLog(): String {
        return GL20.glGetProgramInfoLog(programId, 1024)
    }

    fun validateProgram() {
        GL20.glValidateProgram(programId)
        if (GL20.glGetProgrami(programId, GL20.GL_VALIDATE_STATUS) == 0) {
            throw RuntimeException("Shader validation failed: " + getProgramInfoLog())
        }
    }


    private fun getLoc(uniformName: String): Int {
        val location: Int = GL20.glGetUniformLocation(programId, uniformName)
        if (location == -1) {
            throw RuntimeException("Did not expect uniform with name: " + uniformName);
        }
        return location;
    }

    fun send(uniformName: String, value: Matrix4f) {
        val loc: Int = getLoc(uniformName)
        val buff = BufferUtils.createFloatBuffer(16)
        value.get(buff)
        GL20.glUniformMatrix4fv(loc, false, buff)
    }

    fun send(uniformName: String, value: Vector3f) {
        val loc: Int = getLoc(uniformName)
        GL20.glUniform3f(loc, value.x, value.y, value.z)
    }

    fun terminate() {
        unbind()
        shaderIds.forEach {
            GL20.glDetachShader(programId, it)
            GL20.glDeleteShader(it)
        }

        GL20.glDeleteProgram(programId)
    }

    override fun bind() {
        GL20.glUseProgram(programId)
    }

    override fun unbind() {
        GL20.glUseProgram(0)
    }

    override fun destroy() {
        unbind()
        GL20.glDeleteProgram(programId)
    }

    class ShaderInfo (
        val shaderType: ShaderType,
        val shaderSource: String,
    )

    enum class ShaderType (val glObjectType: Int) {
        VERTEX(GL20.GL_VERTEX_SHADER),
        FRAGMENT(GL20.GL_FRAGMENT_SHADER)
    }
}