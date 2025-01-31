import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.Timer;
import javax.imageio.ImageIO;
import javax.swing.*;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;

import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.util.FPSAnimator;

public class PongGame {
    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale", "1.0");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                MyGui myGUI = new MyGui();
                myGUI.createGUI();
            }
        });
    }
}

class MyGui extends JFrame implements GLEventListener {
    private Game game;

    public void createGUI() {
        setTitle("PongPBR");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);
        GLCanvas canvas = new GLCanvas(caps);
        final FPSAnimator ani = new FPSAnimator(canvas, 120, true);
        canvas.addGLEventListener(this);
        game = new Game();
        canvas.addKeyListener(game);
        ani.start();

        getContentPane().setPreferredSize(new Dimension(800, 450));
        getContentPane().add(canvas);
        pack();
        setVisible(true);
        canvas.requestFocus();
    }

    @Override
    public void init(GLAutoDrawable d) {
        GL3 gl = d.getGL().getGL3();
        gl.glEnable(GL3.GL_DEPTH_TEST);
        float aspect = 16.0f / 9.0f;
        game.projection.setToPerspective((float) Math.toRadians(60.0f), aspect, 1.5f, 5.5f);
        game.init(d);
    }

    @Override
    public void reshape(GLAutoDrawable d, int x, int y, int width, int height) {
        GL3 gl = d.getGL().getGL3();
        float windowAspect = (float) width / (float) height;
        float targetAspect = 16.0f / 9.0f;
        if (windowAspect >= targetAspect) {
            int correctedWidth = Math.round(height * targetAspect);
            int offsetX = Math.round((width - correctedWidth) / 2.0f);
            gl.glViewport(offsetX, 0, correctedWidth, height);
        } else {
            int correctedHeight = Math.round(width / targetAspect);
            int offsetY = Math.round((height - correctedHeight) / 2.0f);
            gl.glViewport(0, offsetY, width, correctedHeight);
        }
    }

    @Override
    public void display(GLAutoDrawable d) {
        game.update();
        game.display(d);
    }

    @Override
    public void dispose(GLAutoDrawable d) {
    }
}

class Game extends KeyAdapter {
    boolean pauseGame = true;
    VboLoader vboLoader = new VboLoader();
    Matrix4f projection = new Matrix4f();

    float[] lightDirection = new float[]{0, 0, -1};
    boolean followBall = false;
    float metallic = 0.0f;
    float roughness = 0.1f;

    Player playerOne;
    Score scoreOne;
    Player playerTwo;
    Score scoreTwo;
    Ball ball;
    PowerUp powerUp;
    Court court;

    Timer timer;
    Shader shader;
    ArrayList<GameObject> gameObjects = new ArrayList<>();

    public Game() {
        ball = new Ball();
        playerOne = new Player(-1.8f, 0f, -90);
        scoreOne = new Score(-0.2f, 0.85f, 0.3f);
        playerTwo = new Player(1.8f, 0f, 90);
        scoreTwo = new Score(0.2f, 0.85f, 0.3f);
        court = new Court();
        powerUp = new PowerUp();

        gameObjects.add(court);
        gameObjects.add(ball);
        gameObjects.add(playerOne);
        gameObjects.add(playerTwo);
        gameObjects.add(scoreOne);
        gameObjects.add(scoreTwo);

        shader = new Shader();
    }

    public void init(GLAutoDrawable d) {
        vboLoader.loadVBO(d, "src/ball.vbo");
        ball.vertBufID = vboLoader.vertBufID;
        ball.vertNo = vboLoader.vertNo;

        vboLoader.loadVBO(d, "src/player.vbo");
        playerOne.vertBufID = vboLoader.vertBufID;
        playerOne.vertNo = vboLoader.vertNo;
        playerTwo.vertBufID = vboLoader.vertBufID;
        playerTwo.vertNo = vboLoader.vertNo;

        vboLoader.loadVBO(d, "src/court.vbo");
        court.vertBufID = vboLoader.vertBufID;
        court.vertNo = vboLoader.vertNo;

        int[] vertBufIDs = new int[4];
        int[] vertNos = new int[4];

        vboLoader.loadVBO(d, "src/0.vbo");
        vertBufIDs[0] = vboLoader.vertBufID;
        vertNos[0] = vboLoader.vertNo;
        vboLoader.loadVBO(d, "src/1.vbo");
        vertBufIDs[1] = vboLoader.vertBufID;
        vertNos[1] = vboLoader.vertNo;
        vboLoader.loadVBO(d, "src/2.vbo");
        vertBufIDs[2] = vboLoader.vertBufID;
        vertNos[2] = vboLoader.vertNo;
        vboLoader.loadVBO(d, "src/3.vbo");
        vertBufIDs[3] = vboLoader.vertBufID;
        vertNos[3] = vboLoader.vertNo;
        scoreOne.vertBufIDs = vertBufIDs;
        scoreOne.vertNos = vertNos;
        scoreOne.setScore(0);
        scoreTwo.vertBufIDs = vertBufIDs;
        scoreTwo.vertNos = vertNos;
        scoreTwo.setScore(0);

        vboLoader.loadVBO(d, "src/box_tri.vbo");
        powerUp.vertBufIDs[0] = vboLoader.vertBufID;
        powerUp.vertNos[0] = vboLoader.vertNo;

        shader.setupShaders(d);

        court.texID = TextureLoader.loadTexture(d, "src/interstellar.png");
        int texId = TextureLoader.loadTexture(d, "src/white.png");
        ball.texID = texId;
        playerOne.texID = texId;
        playerTwo.texID = texId;
        scoreOne.texID = texId;
        scoreTwo.texID = texId;
        powerUp.texIDs[0] = TextureLoader.loadTexture(d, "src/powerup_icons_grow.png");
        powerUp.texIDs[1] = TextureLoader.loadTexture(d, "src/powerup_icons_shrink.png");
        powerUp.texIDs[2] = TextureLoader.loadTexture(d, "src/powerup_icons_star.png");
        powerUp.setType(0);
    }

    public void display(GLAutoDrawable d) {
        GL3 gl = d.getGL().getGL3();

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.glUseProgram(shader.progID);
        gl.glUniformMatrix4fv(shader.projectionLoc, 1, false, projection.get(new float[16]), 0);
        gl.glUniform3f(shader.lightDirectionLoc, lightDirection[0], lightDirection[1], lightDirection[2]);
        gl.glUniform1f(shader.metallicLoc, metallic);
        gl.glUniform1f(shader.roughnessLoc, roughness);

        for (GameObject gameObject : gameObjects) {
            renderGameObject(gl, gameObject);
            if (isShadowCaster(gameObject)) {
                renderShadow(gl, gameObject);
            }
        }
    }

    private boolean isShadowCaster(GameObject obj) {
        return obj instanceof Player || obj instanceof Ball || obj instanceof Score;
    }

    private void renderGameObject(GL3 gl, GameObject gameObject) {
        Matrix4f modelview = new Matrix4f();
        modelview.loadIdentity();
        modelview.translate(gameObject.posX, gameObject.posY, -2.0f, new Matrix4f());
        modelview.scale(gameObject.sizeX, gameObject.sizeY, gameObject.sizeZ, new Matrix4f());
        gameObject.angleZ += gameObject.rotationZ;
        modelview.rotate((float) Math.toRadians(gameObject.angleX), 1, 0, 0, new Matrix4f());
        modelview.rotate((float) Math.toRadians(gameObject.angleY), 0, 1, 0, new Matrix4f());
        modelview.rotate((float) Math.toRadians(gameObject.angleZ), 0, 0, 1, new Matrix4f());
        gameObject.angleY += gameObject.rotationY;
        gl.glUniformMatrix4fv(shader.modelviewLoc, 1, false, modelview.get(new float[16]), 0);

        Matrix4f normalMat = new Matrix4f(modelview);
        normalMat.transpose();
        normalMat.invert();
        gl.glUniformMatrix4fv(shader.normalMatLoc, 1, false, normalMat.get(new float[16]), 0);

        gl.glEnable(GL3.GL_TEXTURE_2D);
        gl.glActiveTexture(GL3.GL_TEXTURE0);
        gl.glBindTexture(GL3.GL_TEXTURE_2D, gameObject.texID);
        gl.glUniform1i(shader.texLoc, 0);

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, gameObject.vertBufID);
        int stride = (3 + 4 + 2 + 3) * Buffers.SIZEOF_FLOAT;
        int offset = 0;

        gl.glVertexAttribPointer(shader.vertexLoc, 3, GL3.GL_FLOAT, false, stride, offset);
        gl.glEnableVertexAttribArray(shader.vertexLoc);

        offset = 3 * Buffers.SIZEOF_FLOAT;
        gl.glVertexAttribPointer(shader.colorLoc, 4, GL3.GL_FLOAT, false, stride, offset);
        gl.glEnableVertexAttribArray(shader.colorLoc);

        offset = (3 + 4) * Buffers.SIZEOF_FLOAT;
        gl.glVertexAttribPointer(shader.texCoordLoc, 2, GL3.GL_FLOAT, false, stride, offset);
        gl.glEnableVertexAttribArray(shader.texCoordLoc);

        offset = (3 + 4 + 2) * Buffers.SIZEOF_FLOAT;
        gl.glVertexAttribPointer(shader.normalLoc, 3, GL3.GL_FLOAT, false, stride, offset);
        gl.glEnableVertexAttribArray(shader.normalLoc);

        gl.glDrawArrays(GL3.GL_TRIANGLES, 0, gameObject.vertNo);
        gl.glDisable(GL3.GL_TEXTURE_2D);
    }

    private void renderShadow(GL3 gl, GameObject gameObject) {
        gl.glUniform1i(shader.isShadowLoc, 1);

        Matrix4f modelview = new Matrix4f();
        modelview.loadIdentity();
        modelview.translate(gameObject.posX, gameObject.posY, -2.25f, new Matrix4f());
        modelview.scale(gameObject.sizeX, gameObject.sizeY, 0.0f, new Matrix4f());
        modelview.rotate((float) Math.toRadians(gameObject.angleX), 1, 0, 0, new Matrix4f());
        modelview.rotate((float) Math.toRadians(gameObject.angleY), 0, 1, 0, new Matrix4f());
        modelview.rotate((float) Math.toRadians(gameObject.angleZ), 0, 0, 1, new Matrix4f());

        gl.glUniformMatrix4fv(shader.modelviewLoc, 1, false, modelview.get(new float[16]), 0);

        Matrix4f normalMat = new Matrix4f(modelview);
        normalMat.transpose();
        normalMat.invert();
        gl.glUniformMatrix4fv(shader.normalMatLoc, 1, false, normalMat.get(new float[16]), 0);

        gl.glEnable(GL3.GL_TEXTURE_2D);
        gl.glActiveTexture(GL3.GL_TEXTURE0);
        gl.glBindTexture(GL3.GL_TEXTURE_2D, gameObject.texID);
        gl.glUniform1i(shader.texLoc, 0);

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, gameObject.vertBufID);
        int stride = (3 + 4 + 2 + 3) * Buffers.SIZEOF_FLOAT;
        int offset = 0;

        gl.glVertexAttribPointer(shader.vertexLoc, 3, GL3.GL_FLOAT, false, stride, offset);
        gl.glEnableVertexAttribArray(shader.vertexLoc);

        offset = 3 * Buffers.SIZEOF_FLOAT;
        gl.glVertexAttribPointer(shader.colorLoc, 4, GL3.GL_FLOAT, false, stride, offset);
        gl.glEnableVertexAttribArray(shader.colorLoc);

        offset = (3 + 4) * Buffers.SIZEOF_FLOAT;
        gl.glVertexAttribPointer(shader.texCoordLoc, 2, GL3.GL_FLOAT, false, stride, offset);
        gl.glEnableVertexAttribArray(shader.texCoordLoc);

        offset = (3 + 4 + 2) * Buffers.SIZEOF_FLOAT;
        gl.glVertexAttribPointer(shader.normalLoc, 3, GL3.GL_FLOAT, false, stride, offset);
        gl.glEnableVertexAttribArray(shader.normalLoc);

        gl.glDrawArrays(GL3.GL_TRIANGLES, 0, gameObject.vertNo);
        gl.glDisable(GL3.GL_TEXTURE_2D);

        gl.glUniform1i(shader.isShadowLoc, 0);
    }

    public void update() {
        if (followBall) {
            lightDirection = new float[]{ball.posX, ball.posX, -2};
        }

        for (GameObject gameObject : gameObjects) {
            gameObject.update();
        }
        checkCollisionBallPlayer();
        checkCollisionBallBorder();
        checkCollisionBallPowerUp();

        if (Util.rand.nextInt(10000) > 9975 && (ball.posY > 0.2f || ball.posY < -0.02f)) {
            spawnPowerUp();
        }
    }

    public void startGame() {
        if (scoreOne.getScore() > 2 || scoreTwo.getScore() > 2) {
            scoreOne.setScore(0);
            scoreTwo.setScore(0);
        }
        ball.velocityX = 0.03f;
        ball.velocityY = 0.015f;
        pauseGame = false;
    }

    public void score(Score score) {
        removePowerUp();
        score.setScore(score.getScore() + 1);
        ball.reset();
        pauseGame = true;
    }

    public void spawnPowerUp() {
        if (!powerUp.spawned && !powerUp.taken) {
            powerUp.setRandomValues();
            gameObjects.add(powerUp);
            powerUp.spawned = true;
        }
    }

    public void removePowerUp() {
        for (int i = 0; i < gameObjects.size(); i++) {
            if (gameObjects.get(i) instanceof PowerUp) {
                gameObjects.remove(i);
                break;
            }
        }
        powerUp.spawned = false;
    }

    public void checkCollisionBallPlayer() {
        if (ball.borderLeft < playerOne.borderRight && ball.borderLeft > playerOne.borderLeft) {
            if (ball.borderDown < playerOne.borderUp && ball.borderUp > playerOne.borderDown) {
                float distanceToCenter = Math.abs(Math.abs(ball.posY) - Math.abs(playerOne.posY));
                if (ball.borderLeft < playerOne.borderRight - distanceToCenter * 0.125f) {
                    ball.posX = (playerOne.borderRight - distanceToCenter * 0.125f) + ball.scaleX;
                    ball.rotationZ = playerOne.velocity * 273;
                    ball.velocityX = -(ball.velocityX + (ball.rotationZ * .0005f));
                    ball.velocityY += (ball.rotationZ * .0015f);
                }
            }
        }

        if (ball.borderRight > playerTwo.borderLeft && ball.borderRight < playerTwo.borderRight) {
            if (ball.borderDown < playerTwo.borderUp && ball.borderUp > playerTwo.borderDown) {
                float distanceToCenter = Math.abs(Math.abs(ball.posY) - Math.abs(playerTwo.posY));
                if (ball.borderRight > playerTwo.borderLeft + distanceToCenter * 0.125f) {
                    ball.posX = (playerTwo.borderLeft + distanceToCenter * 0.125f) - ball.scaleX;
                    ball.rotationZ = playerTwo.velocity * 273;
                    ball.velocityX = -ball.velocityX + (ball.rotationZ * .0005f);
                    ball.velocityY += (ball.rotationZ * .0015f);
                }
            }
        }
    }

    public void checkCollisionBallBorder() {
        if (ball.posX > 1.9f) {
            score(scoreOne);
        }
        if (ball.posX < -1.9f) {
            score(scoreTwo);
        }

        if (ball.posY > 1f) {
            ball.velocityY = -ball.velocityY;
        }
        if (ball.posY < -1f) {
            ball.velocityY = -ball.velocityY;
        }
    }

    public void checkCollisionBallPowerUp() {
        if (powerUp.spawned) {
            if (Math.abs(powerUp.posX - ball.posX) < powerUp.sizeX + ball.sizeX
                    && Math.abs(powerUp.posY - ball.posY) < powerUp.sizeY + ball.sizeY) {
                if (ball.velocityX < 0) {
                    powerUp.applyPowerUp(playerTwo, playerOne);
                } else {
                    powerUp.applyPowerUp(playerOne, playerTwo);
                }
                timer = new Timer(true);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        powerUp.removePowerUp();
                        powerUp.taken = false;
                        timer.cancel();
                    }
                }, 4000);

                removePowerUp();
                powerUp.taken = true;
            }
        }
    }

    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
                playerOne.moveUp = true;
                break;
            case KeyEvent.VK_S:
                playerOne.moveDown = true;
                break;
            case KeyEvent.VK_P:
                playerTwo.moveUp = true;
                break;
            case KeyEvent.VK_L:
                playerTwo.moveDown = true;
                break;
            case KeyEvent.VK_SPACE:
                if (pauseGame) {
                    startGame();
                }
                break;
            case KeyEvent.VK_0:
                lightDirection = new float[]{0, 0, -1};
                followBall = false;
                break;
            case KeyEvent.VK_1:
                lightDirection = new float[]{0, -1, 0};
                followBall = false;
                break;
            case KeyEvent.VK_2:
                lightDirection = new float[]{0, 1, 0};
                followBall = false;
                break;
            case KeyEvent.VK_3:
                lightDirection = new float[]{-1, -1, 0};
                followBall = false;
                break;
            case KeyEvent.VK_4:
                followBall = true;
                break;
            case KeyEvent.VK_5:
                metallic = 0.0f;
                break;
            case KeyEvent.VK_6:
                metallic = 1.0f;
                break;
            case KeyEvent.VK_7:
                roughness = 0.1f;
                break;
            case KeyEvent.VK_8:
                roughness = 0.2f;
                break;
        }
    }

    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
                playerOne.moveUp = false;
                break;
            case KeyEvent.VK_S:
                playerOne.moveDown = false;
                break;
            case KeyEvent.VK_P:
                playerTwo.moveUp = false;
                break;
            case KeyEvent.VK_L:
                playerTwo.moveDown = false;
                break;
        }
    }
}

abstract class GameObject {
    int vertBufID;
    int vertNo;
    int texID;

    float angleX, angleY, angleZ;
    float rotationY, rotationZ;
    float posX, posY;
    float sizeX, sizeY, sizeZ;

    public void update() {
    }
}

class Player extends GameObject {
    boolean moveUp, moveDown = false;
    float ACCELERATION_VALUE = 0.012f;
    float acceleration;
    float velocity;
    float borderLeft, borderRight, borderUp, borderDown;
    float scaleX, scaleY, scaleZ;

    public Player(float posX, float posY, float angleZ) {
        this.scaleX = 0.35f;
        this.scaleY = 0.35f;
        this.scaleZ = 0.35f;
        this.sizeX = this.scaleX * 2;
        this.sizeY = this.scaleY * 2;
        this.sizeZ = this.scaleZ * 2;
        this.posX = posX;
        this.posY = posY;
        this.angleZ = angleZ;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
        this.sizeY = this.scaleY * 2;
    }

    public void update() {
        acceleration = 0.0f;
        if (moveUp) {
            acceleration += ACCELERATION_VALUE;
        }
        if (moveDown) {
            acceleration += -ACCELERATION_VALUE;
        }

        velocity += acceleration;
        velocity *= 0.75f;
        this.posY += velocity;

        if (this.posY >= 0.8f) {
            this.posY = 0.8f;
        }
        if (this.posY <= -0.8f) {
            this.posY = -0.8f;
        }

        this.borderLeft = this.posX - this.scaleX / 4f;
        this.borderRight = this.posX + this.scaleX / 4f;

        this.borderUp = this.posY + this.scaleY;
        this.borderDown = this.posY - this.scaleY;
    }
}

class Ball extends GameObject {
    float velocityX, velocityY;
    float borderLeft, borderRight, borderUp, borderDown;
    float scaleX, scaleY, scaleZ;

    public Ball() {
        this.scaleX = this.scaleY = this.scaleZ = 0.075f;
        this.sizeX = this.sizeY = this.sizeZ = this.scaleX * 2;
    }

    public void update() {
        this.posX += velocityX;
        this.posY += velocityY;

        this.borderLeft = this.posX - this.scaleX;
        this.borderRight = this.posX + this.scaleX;
        this.borderUp = this.posY + this.scaleY;
        this.borderDown = this.posY - this.scaleY;
    }

    public void reset() {
        this.velocityX = 0;
        this.velocityY = 0;
        this.posX = 0;
        this.posY = 0;
        this.angleZ = 0;
        this.rotationZ = 0;
    }
}

class PowerUp extends GameObject {
    float velocity;
    int type;
    int[] texIDs = new int[3];
    int[] vertBufIDs = new int[1];
    int[] vertNos = new int[1];
    Player lastConsumerPlayer;
    Player lastOtherPlayer;
    boolean spawned;
    boolean taken;

    public PowerUp() {
        this.sizeX = this.sizeY = this.sizeZ = 0.1f;
        spawned = false;
        taken = false;
    }

    public void setType(int powerUpType) {
        type = powerUpType;
        this.texID = texIDs[powerUpType];
        this.vertBufID = vertBufIDs[0];
        this.vertNo = vertNos[0];
    }

    public void setRandomValues() {
        velocity = Util.rand.nextInt(1000) / 1000f * 0.01f;
        var randomInt = Util.rand.nextInt(2);
        setType(randomInt);
    }

    public void update() {
        if (posY > 1f) {
            posY = 1f;
            velocity = -velocity;
        }
        if (posY < -1f) {
            posY = -1f;
            velocity = -velocity;
        }
        posY += velocity;
    }

    public void applyPowerUp(Player consumer, Player other) {
        switch (type) {
            case 0:
                consumer.setScaleY(consumer.scaleY * 2);
                break;
            case 1:
                other.setScaleY(other.scaleY / 2);
                break;
            case 2:
                consumer.ACCELERATION_VALUE *= 2;
                break;
        }
        lastConsumerPlayer = consumer;
        lastOtherPlayer = other;
    }

    public void removePowerUp() {
        switch (type) {
            case 0:
                lastConsumerPlayer.setScaleY(lastConsumerPlayer.scaleY / 2);
                break;
            case 1:
                lastOtherPlayer.setScaleY(lastOtherPlayer.scaleY * 2);
                break;
            case 2:
                lastConsumerPlayer.ACCELERATION_VALUE /= 2;
                break;
        }
    }
}

class Court extends GameObject {
    public Court() {
        this.rotationY = -0.01f;
        this.sizeX = this.sizeY = this.sizeZ = 2f;
    }

    public void update() {
        this.angleY += rotationY;
    }
}

class Score extends GameObject {
    private int score = 0;
    int[] vertBufIDs;
    int[] vertNos;

    public Score(float posX, float posY, float size) {
        this.posX = posX;
        this.posY = posY;
        this.sizeX = size;
        this.sizeY = size;
        this.sizeZ = size;
    }

    public void setScore(int score) {
        if (score > 3) {
            return;
        }
        this.score = score;
        if (score < vertBufIDs.length) {
            vertBufID = vertBufIDs[score];
            vertNo = vertNos[score];
        }
    }

    public int getScore() {
        return this.score;
    }
}

class Util {
    static Random rand = new Random();
}

class TextureLoader {
    static int loadTexture(GLAutoDrawable d, String filename) {
        GL3 gl = d.getGL().getGL3();

        int width;
        int height;
        int level = 0;
        int border = 0;

        try {
            FileInputStream fileInputStream = new FileInputStream(filename);
            BufferedImage bufferedImage = ImageIO.read(fileInputStream);
            fileInputStream.close();

            width = bufferedImage.getWidth();
            height = bufferedImage.getHeight();
            int[] pixelIntData = new int[width * height];
            bufferedImage.getRGB(0, 0, width, height, pixelIntData, 0, width);
            ByteBuffer buffer = ByteBuffer.allocateDirect(pixelIntData.length * 4);
            buffer.order(ByteOrder.nativeOrder());

            for (int y = 0; y < height; y++) {
                int k = (height - 1 - y) * width;
                for (int x = 0; x < width; x++) {
                    buffer.put((byte) (pixelIntData[k] >>> 16));
                    buffer.put((byte) (pixelIntData[k] >>> 8));
                    buffer.put((byte) (pixelIntData[k]));
                    buffer.put((byte) (pixelIntData[k] >>> 24));
                    k++;
                }
            }
            buffer.rewind();

            gl.glPixelStorei(GL3.GL_UNPACK_ALIGNMENT, 1);

            final int[] textureID = new int[1];
            gl.glGenTextures(1, textureID, 0);

            gl.glBindTexture(GL3.GL_TEXTURE_2D, textureID[0]);
            gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
            gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);

            gl.glTexImage2D(GL3.GL_TEXTURE_2D, level, GL3.GL_RGB, width, height, border, GL3.GL_RGBA,
                    GL3.GL_UNSIGNED_BYTE, buffer);

            return textureID[0];
        } catch (FileNotFoundException e) {
            System.out.println("Can not find texture data file " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }
}

class VboLoader {
    int vertBufID;
    int vertNo;

    public void loadVBO(GLAutoDrawable d, String filename) {
        GL3 gl = d.getGL().getGL3();
        int perVertexFloats = (3 + 4 + 2 + 3);
        float[] vertexData = loadVertexData(filename, perVertexFloats);

        int[] vboID = new int[1];
        gl.glGenBuffers(1, vboID, 0);
        vertBufID = vboID[0];
        vertNo = vertexData.length / perVertexFloats;
        FloatBuffer dataIn = Buffers.newDirectFloatBuffer(vertexData.length);
        dataIn.put(vertexData);
        dataIn.flip();

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertBufID);
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, (long) dataIn.capacity() * Buffers.SIZEOF_FLOAT, dataIn, GL3.GL_STATIC_DRAW);
    }

    private float[] loadVertexData(String filename, int perVertexFloats) {
        float[] floatArray = new float[0];

        try {
            InputStream is = new FileInputStream(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = br.readLine();
            if (line != null) {
                int vertSize = Integer.parseInt(line);
                floatArray = new float[vertSize];
            }
            int i = 0;
            while ((line = br.readLine()) != null && i < floatArray.length) {
                floatArray[i] = Float.parseFloat(line);
                i++;
            }
            if (i != floatArray.length || (floatArray.length % perVertexFloats) != 0) {
                floatArray = new float[0];
            }
            br.close();
        } catch (FileNotFoundException e) {
            System.out.println("Can not find vbo data file " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return floatArray;
    }
}

class Shader {
    int progID = 0;

    int vertexLoc = 0;
    int colorLoc = 0;
    int texCoordLoc = 0;
    int normalLoc = 0;
    int projectionLoc = 0;
    int modelviewLoc = 0;
    int normalMatLoc = 0;
    int texLoc = 0;
    int lightDirectionLoc = 0;
    int metallicLoc = 0;
    int roughnessLoc = 0;
    int isShadowLoc = 0;

    public void setupShaders(GLAutoDrawable d) {
        GL3 gl = d.getGL().getGL3();

        int textVertID = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
        int textFragID = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);

        String[] vs = new String[]{
                """
                #version 140

                in vec3 inputPosition;
                in vec4 inputColor;
                in vec2 inputTexCoord;
                in vec3 inputNormal;

                uniform mat4 projection;
                uniform mat4 modelview;
                uniform mat4 normalMat;

                out vec3 forFragColor;
                out vec2 forFragTexCoord;
                out vec3 normal;
                out vec3 vertPos;

                void main(){
                    forFragColor = inputColor.rgb;
                    forFragTexCoord = inputTexCoord;
                    normal = (normalMat * vec4(inputNormal, 0.0)).xyz;
                    vec4 vertPos4 = modelview * vec4(inputPosition, 1.0);
                    vertPos = vec3(vertPos4) / vertPos4.w;
                    gl_Position =  projection * modelview * vec4(inputPosition, 1.0);
                }
                """
        };

        String[] fs = new String[]{
                """
                #version 140
                out vec4 outputColor;

                in vec2 forFragTexCoord;
                in vec3 normal;
                in vec3 vertPos;
                in vec3 forFragColor;

                uniform sampler2D myTexture;
                uniform vec3 lightDirection;
                uniform float metallic;
                uniform float roughness;
                uniform int isShadow;

                const vec4 lightColor = vec4(1.0, 1.0, 1.0, 1.0);
                const vec3 ambientLight = vec3(0.1, 0.1, 0.1);
                const float reflectance = 0.5f;
                const float irradiPerp = 5.0f;

                #define RECIPROCAL_PI 0.3183098861837907

                vec3 fresnelSchlick(float cosTheta, vec3 F0) {
                  return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
                }

                float D_GGX(float NoH, float roughness) {
                  float alpha = roughness * roughness;
                  float alpha2 = alpha * alpha;
                  float NoH2 = NoH * NoH;
                  float b = (NoH2 * (alpha2 - 1.0) + 1.0);
                  return alpha2 * RECIPROCAL_PI / (b * b);
                }

                float G1_GGX_Schlick(float NoV, float roughness) {
                  float alpha = roughness * roughness;
                  float k = alpha / 2.0;
                  return max(NoV, 0.001) / (NoV * (1.0 - k) + k);
                }

                        float G_Smith(float NoV, float NoL, float roughness) {
                  return G1_GGX_Schlick(NoL, roughness) * G1_GGX_Schlick(NoV, roughness);
                }

                float fresnelSchlick90(float cosTheta, float F0, float F90) {
                  return F0 + (F90 - F0) * pow(1.0 - cosTheta, 5.0);
                }

                vec3 microfacetBRDF(in vec3 L, in vec3 V, in vec3 N,
                                    in float metallic, in float roughness, in vec3 baseColor, in float reflectance) {
                  vec3 H = normalize(V + L);
                  float NoV = clamp(dot(N, V), 0.0, 1.0);
                  float NoL = clamp(dot(N, L), 0.0, 1.0);
                  float NoH = clamp(dot(N, H), 0.0, 1.0);
                  float VoH = clamp(dot(V, H), 0.0, 1.0);
                  vec3 f0 = vec3(0.16 * (reflectance * reflectance));
                  f0 = mix(f0, baseColor, metallic);
                  vec3 F = fresnelSchlick(VoH, f0);
                  float D = D_GGX(NoH, roughness);
                  float G = G_Smith(NoV, NoL, roughness);
                  vec3 spec = (F * D * G) / (4.0 * max(NoV, 0.001) * max(NoL, 0.001));
                  vec3 rhoD = baseColor;
                  rhoD *= vec3(1.0) - F;
                  rhoD *= (1.0 - metallic);
                  vec3 diff = rhoD * RECIPROCAL_PI;
                  return diff + spec;
                }

                void main() {
                    if (isShadow == 1) {
                        outputColor = vec4(0.0, 0.0, 0.0, 1.0); // Draw shadow in black
                        return;
                    }

                    vec3 n = normalize(normal.xyz);
                    vec3 lightDir = normalize(-lightDirection);
                    vec3 viewDir = normalize(-vertPos);

                    vec3 textureColor = texture(myTexture, forFragTexCoord).rgb;
                    vec3 baseColor = forFragColor * textureColor;
                    baseColor = pow(baseColor, vec3(2.2)); // Gamma correction

                    vec3 radiance = ambientLight * baseColor;
                    float irradiance = max(dot(lightDir, n), 0.0) * irradiPerp;
                    if (irradiance > 0.0) {
                        vec3 brdf = microfacetBRDF(lightDir, viewDir, n, metallic, roughness, baseColor, reflectance);
                        radiance += brdf * irradiance * lightColor.rgb;
                    }

                    radiance = pow(radiance, vec3(1.0 / 2.2)); // Gamma correction
                    outputColor = vec4(radiance, 1.0);
                }
                """
        };

        gl.glShaderSource(textVertID, 1, vs, null, 0);
        gl.glShaderSource(textFragID, 1, fs, null, 0);

        gl.glCompileShader(textVertID);
        gl.glCompileShader(textFragID);

        printShaderInfoLog(d, textVertID);
        printShaderInfoLog(d, textFragID);

        progID = gl.glCreateProgram();
        gl.glAttachShader(progID, textVertID);
        gl.glAttachShader(progID, textFragID);

        gl.glBindFragDataLocation(progID, 0, "outputColor");

        gl.glLinkProgram(progID);
        printProgramInfoLog(d, progID);

        vertexLoc = gl.glGetAttribLocation(progID, "inputPosition");
        colorLoc = gl.glGetAttribLocation(progID, "inputColor");
        texCoordLoc = gl.glGetAttribLocation(progID, "inputTexCoord");
        normalLoc = gl.glGetAttribLocation(progID, "inputNormal");

        projectionLoc = gl.glGetUniformLocation(progID, "projection");
        modelviewLoc = gl.glGetUniformLocation(progID, "modelview");
        normalMatLoc = gl.glGetUniformLocation(progID, "normalMat");
        texLoc = gl.glGetUniformLocation(progID, "myTexture");
        lightDirectionLoc = gl.glGetUniformLocation(progID, "lightDirection");
        metallicLoc = gl.glGetUniformLocation(progID, "metallic");
        roughnessLoc = gl.glGetUniformLocation(progID, "roughness");
        isShadowLoc = gl.glGetUniformLocation(progID, "isShadow");
    }

    private static void printShaderInfoLog(GLAutoDrawable d, int obj) {
        GL3 gl = d.getGL().getGL3();
        IntBuffer infoLogLengthBuf = IntBuffer.allocate(1);
        int infoLogLength;
        gl.glGetShaderiv(obj, GL3.GL_INFO_LOG_LENGTH, infoLogLengthBuf);
        infoLogLength = infoLogLengthBuf.get(0);
        if (infoLogLength > 0) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(infoLogLength);
            gl.glGetShaderInfoLog(obj, infoLogLength, infoLogLengthBuf, byteBuffer);
            for (byte b : byteBuffer.array()) {
                System.err.print((char) b);
            }
        }
    }

    private static void printProgramInfoLog(GLAutoDrawable d, int obj) {
        GL3 gl = d.getGL().getGL3();
        IntBuffer infoLogLengthBuf = IntBuffer.allocate(1);
        int infoLogLength;
        gl.glGetProgramiv(obj, GL3.GL_INFO_LOG_LENGTH, infoLogLengthBuf);
        infoLogLength = infoLogLengthBuf.get(0);
        if (infoLogLength > 0) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(infoLogLength);
            gl.glGetProgramInfoLog(obj, infoLogLength, infoLogLengthBuf, byteBuffer);
            for (byte b : byteBuffer.array()) {
                System.err.print((char) b);
            }
        }
    }
}