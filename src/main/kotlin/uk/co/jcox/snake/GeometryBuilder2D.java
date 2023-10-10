package uk.co.jcox.snake;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class GeometryBuilder2D {

    private GeometryBuilder2D() {

    }

    public static Geometry rectangle(float x, float y) {


        final float[] vertices = {
                //X   Y     Z
                x, y, 0.0f,       //Top Right
                x, 0, 0.0f,       //Bottom Right
                0, 0, 0.0f,       //Bottom Left
                0, y, 0.0f,       //Top Left
        };

        final int[] indices = {
                0, 1, 3,
                1, 2, 3,
        };

        final int vertexArray = createVertexArrays(vertices, indices);

        return new Geometry(vertexArray, indices.length);
    }

    private static int createVertexArrays(float[] vertices, int[] indices) {
        final int VAO = GL30.glGenVertexArrays();
        final int VBO = GL15.glGenBuffers();
        final int EBO = GL15.glGenBuffers();

        GL30.glBindVertexArray(VAO);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, VBO);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, EBO);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        return VAO;
    }
}
