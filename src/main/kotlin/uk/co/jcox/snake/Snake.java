package uk.co.jcox.snake;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class Snake {

    private int gridX = 30;
    private int gridY = 30;

    private final Window window = new Window("LWJGL OpenGL Snake2D", 1000, 1000);
    private final Renderer renderer = new Renderer();
    private ShaderProgram program;
    private final Random random = new Random();

    private float movementAccumulator = 0.0f;

    private final List<Vector3f> trackedHeadPositions = new ArrayList<>();
    private Movement snakeDirection = Movement.UP;
    private final Vector3f snakeHeadPosition = new Vector3f( gridX / 2, gridY /2 , 0.0f);
    private int snakeLength = 0;

    private Vector3f foodPosition = null;

    private ObjRepresentable snakeHead;
    private ObjRepresentable snakeBody;
    private ObjRepresentable foodItem;


    public static final String MODEL_UNIFORM = "model";

    public void start() {

        if (! this.window.createGlContext()) {
            throw new IllegalStateException("Could not establish the OpenGL context");
        }
        System.out.println("Created an OpenGL context");

        this.renderer.setup();
        String vertShadSrc = "";
        String fragShadSrc = "";

        final Properties properties = new Properties();
        try (FileInputStream stream = new FileInputStream("data/options.properties")) {

            properties.load(stream);
            gridX = Integer.parseInt(properties.getProperty("GridSizeX"));
            gridY = Integer.parseInt(properties.getProperty("GridSizeY"));

            vertShadSrc = Files.readString(Paths.get("data/shaders/default.vsh"));
            fragShadSrc = Files.readString(Paths.get("data/shaders/default.fsh"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.program = new ShaderProgram(
                new ShaderProgram.ShaderInfo(ShaderProgram.ShaderType.VERTEX, vertShadSrc),
                new ShaderProgram.ShaderInfo(ShaderProgram.ShaderType.FRAGMENT, fragShadSrc)
        );

        System.out.println("Successfully compiled and loaded shaders");

        Geometry square = GeometryBuilder2D.rectangle(1, 1);
        this.snakeHead = new ObjRepresentable(new Material(new Vector3f(1.0f, 1.0f, 0.8f)), square);
        this.snakeBody = new ObjRepresentable(new Material(new Vector3f(1.0f, 1.0f, 0.5f)), square);
        this.foodItem = new ObjRepresentable(new Material(new Vector3f(0.2f, 1.0f, 0.2f)), square);

        this.renderer.setClearColour(11, 11,11);
        program.send("projection", new Matrix4f().ortho(0, gridX, 0, gridY, 0, 1));

        runRenderLoop();
    }

    private void runRenderLoop() {
        double lastFrameTime = 0.0;
        double deltaTime;

        while (!this.window.shouldClose()) {
            double currentFrame = window.getTimeElapsed();
            deltaTime = currentFrame - lastFrameTime;
            lastFrameTime = currentFrame;

            render();
            keyInput();
            update(deltaTime);

            window.setInfo(" {FPS: " + 1 / deltaTime + " } ");
        }

        System.out.println("Cleaning up the shaders and window..");
        this.window.terminate();
        this.program.terminate();

        System.out.println("You have earned " + snakeLength + " points.");
        System.out.println("Your snake currently occupies the points: ");
        for (Vector3f trackedHeadPosition : trackedHeadPositions) {
            System.out.println("Position: " + trackedHeadPosition.x + ", " + trackedHeadPosition.y);
        }
    }


    private void render() {
        this.renderer.clearScreen();

        //Render the head
        program.send(MODEL_UNIFORM, new Matrix4f().translate(snakeHeadPosition));
        this.renderer.draw(snakeHead, program, window.getWidth(), window.getHeight());

        //Render the rest of the body
        for (Vector3f bodyPartPos : trackedHeadPositions) {
            program.send(MODEL_UNIFORM, new Matrix4f().translate(bodyPartPos));
            this.renderer.draw(snakeBody, program, window.getWidth(), window.getHeight());
        }

        //Any food items on the map
        if (this.foodPosition != null) {
            program.send(MODEL_UNIFORM, new Matrix4f().translate(foodPosition));
            this.renderer.draw(foodItem, program, window.getWidth(), window.getHeight());
        }
    }


    private void keyInput() {
        if (window.isPressed(GLFW.GLFW_KEY_UP)) {
            snakeDirection = Movement.UP;
        }
        if (window.isPressed(GLFW.GLFW_KEY_DOWN)) {
            snakeDirection = Movement.DOWN;
        }
        if (window.isPressed(GLFW.GLFW_KEY_LEFT)) {
            snakeDirection = Movement.LEFT;
        }
        if (window.isPressed(GLFW.GLFW_KEY_RIGHT)) {
            snakeDirection = Movement.RIGHT;
        }
    }

    private void update(double deltaTime) {
        this.window.runWindowUpdates();
        this.updateMovement(deltaTime);
        this.placeFood();

        this.updateCollisions();
    }

    private void updateMovement(double deltaTime) {
        //Info: Movement is discrete, therefore the head will move 1 unit every 0.25 seconds
        //With no motion
        movementAccumulator += (float) deltaTime;
        if (movementAccumulator >= 0.10) {

            if (trackedHeadPositions.size() < snakeLength) {
                this.trackedHeadPositions.add(0, new Vector3f(snakeHeadPosition));
            } else if (!trackedHeadPositions.isEmpty()) {
                this.trackedHeadPositions.remove(this.trackedHeadPositions.size() - 1);
                this.trackedHeadPositions.add(0, new Vector3f(snakeHeadPosition));
            }


            snakeHeadPosition.add(snakeDirection.velocity);
            movementAccumulator = 0;
        }
    }


    private void updateCollisions() {
        //The playable grid starts at 0, and ends at ${GRID_X} -> Same with Y
        //Additionally the geometry for a snake part, starts drawing as left =0, and right = 1
        //If any collision is detected, the game is closed via the GLFW window
        if ( (snakeHeadPosition.x >= gridX) || (snakeHeadPosition.x <= -1) || (snakeHeadPosition.y >= gridY) || (snakeHeadPosition.y <= -1) ) {
            window.exit();
        }

        //Check to see if the snake touches itself
        if (trackedHeadPositions.contains(snakeHeadPosition)) {
            window.exit();
        }

        //Check to see if the snake head collides with food
        if (snakeHeadPosition.equals(foodPosition)) {
            this.foodPosition = null;
            this.snakeLength++;
            System.out.println("One point awarded");
        }
    }


    private void placeFood() {
        if (this.foodPosition == null) {
            this.foodPosition = findNextFoodSlot();
        }
    }


    //A recursive function that finds the next position to place food.
    //If the chosen coordinates are in the snake, the function is rerun recursively
    private Vector3f findNextFoodSlot() {
        final Vector3f foodToPlace = new Vector3f(random.nextInt(gridX), random.nextInt(gridY), 0.0f);
        if (trackedHeadPositions.contains(foodToPlace)) {
            return findNextFoodSlot();
        }
        return foodToPlace;
    }



    enum Movement {
        UP(new Vector3f(0.0f, 1.0f, 0.0f)),
        DOWN(new Vector3f(0.0f, -1.0f, 0.0f)),
        LEFT(new Vector3f(-1.0f, 0.0f, 0.0f)),
        RIGHT(new Vector3f(1.0f, 0.0f, 0.0f)),
        ;

        private final Vector3f velocity;

        Movement(Vector3f velocity) {
            this.velocity = velocity;
        }
    }
}
