package engine.collision;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class CollisionTester {

    private static int moveDirY = 0;
    private static int moveDirX = 0;

    private static float moveSpeed = 360 ;
    private static float size = 150;
    private static  boolean seperate =false;
    private static int sleep = 16;
    public static void main(String args[]) {
        JFrame frame = new JFrame("Test Collisions!");
        frame.setSize(1920, 1080);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addKeyListener(kl);
        frame.setVisible(true);

        Collider movin = new Collider();
        movin.boundingBox = new Box();
        movin.boundingBox.transform(new Matrix4f().scale(size+10));
        Box smol1 = new Box();
        smol1.transform(new Matrix4f().scale(size/2f));
        movin.allTheShapes.add(smol1);
        Box smol2 = new Box();
        smol2.transform(new Matrix4f().scale(size/2f));
        smol2.transform(new Matrix4f().translate(size/2f+10,size/2f+10,0));
        movin.allTheShapes.add(smol2);
        movin.transform(new Matrix4f().rotateZ(0.3f));
        movin.transform(new Matrix4f().translate(760f,  700,0));

        Collider staticShape = new Collider();
        staticShape.boundingBox = new Box();
       // staticShape.boundingBox.transform(new Matrix4f().translate(-0.5f,0.5f,-0.5f));
        staticShape.boundingBox.transform(new Matrix4f().scale(size));
        staticShape.boundingBox.transform(new Matrix4f().translate(size / 2f, size / 2f, 0));
        staticShape.boundingBox.transform(new Matrix4f().translate(500, 500, 0));
        staticShape.allTheShapes.add(staticShape.boundingBox.clone());


        while (true) {
            float dt = 1f / 60f;
            Vector3f velocity = new Vector3f(moveDirX, moveDirY, 0);
            if (velocity.lengthSquared() != 0)
                velocity.normalize().mul(moveSpeed);
            Vector3f mtv = SAT.getMTV(movin,velocity, staticShape,dt);
            movin.transform(new Matrix4f().translate(velocity.x * dt, velocity.y * dt, 0));
            System.out.println(mtv);
            if (mtv != null) {
                movin.transform(new Matrix4f().translate(mtv));
                seperate=false;
            }
            Graphics g = frame.getGraphics();
            g.clearRect(0, 0, 1920, 1080);
            g.setColor(Color.BLACK);
            Color movinColor = new Color(0, 0, 255, 125);
            Color staticColor = new Color(0, 255, 0, 125);
            draw(staticShape,staticColor, g, new Vector3f(300, 300, 0),movin,movinColor);
            draw(movin,movinColor, g, new Vector3f(305, 305, 0),staticShape,staticColor);
            g.dispose();
            Toolkit.getDefaultToolkit().sync();
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void draw(Collider collider,Color ownColor, Graphics g, Vector3f offset,Collider other,Color otherColor) {
      //  draw(collider.boundingBox,ownColor, g, offset,other,otherColor);
        for (CollisionShape cs : collider.allTheShapes) {
            draw(cs,ownColor, g, offset,other,otherColor);
        }
    }

    private static void draw(CollisionShape aabb,Color ownColor, Graphics g, Vector3f offset, Collider other,Color otherColor) {
        g.setColor(ownColor);
        for (int i = 0; i < aabb.cornerPoints.length; i++) {
            Vector3f p1 = aabb.cornerPoints[i];
            for (int j = i; j < aabb.cornerPoints.length; j++) {
                Vector3f p = aabb.cornerPoints[j];
                g.drawLine((int) p.x, (int) p.y, (int) p1.x, (int) p1.y);
            }
        }
        for (Vector3f axis : aabb.getAxis()) {
            drawAxis(axis,ownColor, g, offset, aabb.projectOnAxis(axis),other,otherColor);
        }
    }

    private static void drawAxis(Vector3f axis,Color ownColor, Graphics gr, Vector3f offset, Vector2f projection1, Collider other, Color otherColor) {
        Graphics2D g = (Graphics2D) gr;
        Vector3f startPoint = new Vector3f(axis).mulAdd(-2000f, offset);
        Vector3f endPoint = new Vector3f(axis).mulAdd(2000f, offset);
        g.drawLine((int) startPoint.x, (int) startPoint.y, (int) endPoint.x, (int) endPoint.y);
        float lineStroke = 5f;
        g.setColor(ownColor);
        g.setStroke(new BasicStroke(lineStroke));
        Vector3f oneMinusAxis = new Vector3f(1).sub(axis).mul(offset);
        Vector3f projMin = new Vector3f(axis).mulAdd(projection1.x + lineStroke / 2f, oneMinusAxis);
        Vector3f projMax = new Vector3f(axis).mulAdd(projection1.y - lineStroke / 2f, oneMinusAxis);
        g.drawLine((int) projMin.x, (int) projMin.y, (int) projMax.x, (int) projMax.y);
        g.setColor(otherColor);
        for(CollisionShape cs:other.allTheShapes){
        Vector2f projection2 = cs.projectOnAxis(axis);
        Vector3f projMin2 = new Vector3f(axis).mulAdd(projection2.x + lineStroke / 2f, oneMinusAxis);
        Vector3f projMax2 = new Vector3f(axis).mulAdd(projection2.y - lineStroke / 2f, oneMinusAxis);
        g.drawLine((int) projMin2.x, (int) projMin2.y, (int) projMax2.x, (int) projMax2.y);
        }
        g.setStroke(new BasicStroke(1f));

    }

    private static KeyListener kl = new KeyListener() {
        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int kc = e.getKeyCode();
            if (kc == KeyEvent.VK_UP) {
                moveDirY = -1;
            }
            if (kc == KeyEvent.VK_DOWN) {
                moveDirY = 1;
            }
            if (kc == KeyEvent.VK_LEFT) {
                moveDirX = -1;
            }
            if (kc == KeyEvent.VK_RIGHT) {
                moveDirX = 1;
            }
            if(kc==KeyEvent.VK_S){
                seperate=true;
            } if(kc==KeyEvent.VK_PLUS){
                sleep=16;
            } if(kc==KeyEvent.VK_MINUS){
                sleep=200;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
                moveDirY = 0;
            }
            if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
                moveDirX = 0;
            }
        }
    };


}
