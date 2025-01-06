import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import java.util.*;
import java.util.List; 
import java.util.Scanner;
import java.util.ArrayList;
import javax.swing.Timer;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Project1 {
    public static void main(String[] args) {
        JFrame jf = new JFrame("Project 1");
        DrawingPanel dp = (args.length > 1)?
            new DrawingPanel(args[0]) :
            new DrawingPanel("key.vert");

        jf.add(dp);
        jf.setSize(800,600);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setLocationRelativeTo(null);
        jf.setVisible(true);
    }
}

class DrawingPanel extends JPanel {
    private List<List<Point2D.Double>> components; 
    private float scalingfactor;
    private Timer timer; 

    public DrawingPanel(String fname) {
        components = new ArrayList<>();
        //scalingfactor = 1400.0f; // superior
        scalingfactor = 8.0f;    // key
        //scalingfactor = 500.0f;    // disk
        loadVertFile(fname);

        timer = new Timer(10, e -> {
            evolveByDiscreteObjectiveFlow(0.1);
            repaint();
        });
        //timer.start(); 
    }

    private void loadVertFile(String fname) {
        Path fp = Paths.get(fname);
        try (Scanner input = new Scanner(fp)) {
            int nComponent = input.nextInt();
            for (int c = 0; c < nComponent; c++) {
                int nv = input.nextInt();
                List<Point2D.Double> points = new ArrayList<>();
                for (int i = 0; i < nv; i++) {
                    double x = input.nextDouble();
                    double y = input.nextDouble();
                    points.add(new Point2D.Double(x, y));
                }
                components.add(points);
            }

        } catch (Exception e) {
            System.err.println("Failed to load .vert file: " + e.getMessage());
        }
    }

    // draw
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        int height = getHeight();
        int width = getWidth();

        AffineTransform t1 = new AffineTransform();
        //t1.translate(width-800, height+600); // superior
        t1.translate(width+400, height-200); // key
        //t1.translate(width, height);          // disk
        t1.rotate(Math.PI/2);               // key
        t1.scale(scalingfactor, scalingfactor);
        g2d.setTransform(t1);

        g2d.setStroke(new BasicStroke(2.0f/scalingfactor));
        g2d.setColor(Color.BLACK);

        for (List<Point2D.Double> comp : components) {
            drawComponent(g2d, comp);
        }

        g2d.setColor(Color.RED);
        for (List<Point2D.Double> comp : components) {
            drawTangentAndNormal(g2d, comp);
        }
    }


    private void drawComponent(Graphics2D g2d, List<Point2D.Double> comp) {
        Path2D.Double path = new Path2D.Double();
        if (comp.size() > 0) {
            path.moveTo(comp.get(0).x, comp.get(0).y);
            for (int i = 1; i < comp.size(); i++) {
                path.lineTo(comp.get(i).x, comp.get(i).y);
            }
            path.closePath();
        }

        g2d.draw(path);
    }


    private void drawTangentAndNormal(Graphics2D g2d, List<Point2D.Double> comp) {
        if (comp.size() < 2) return;

        for (int i = 0; i < comp.size(); i++) {
            //if(i % 2 == 0){
            //    continue;
            //}
            Point2D.Double prev = comp.get((i - 1 + comp.size()) % comp.size());
            Point2D.Double curr = comp.get(i);
            Point2D.Double next = comp.get((i + 1) % comp.size());

            double tx = next.x - prev.x;
            double ty = next.y - prev.y;
            double len = Math.sqrt(tx*tx + ty*ty);
            if (len < 1e-10) continue;
            tx /= len;
            ty /= len;

            double nx = -ty; 
            double ny =  tx;
            double nlen = Math.sqrt(nx*nx + ny*ny);
            nx /= nlen;
            ny /= nlen;

            //double scale = 0.05;//superior
            double scale = 10.0;//key
            //double scale = 0.2;//disk

            double curvature = computeDiscreteCurvature(prev, curr, next);
            System.out.println("Curvature at point " + i + ": " + curvature);
            

            // draw Unit Tangent
            //drawArrow(g2d, curr.x, curr.y, curr.x + tx*scale, curr.y + ty*scale, Color.RED);
            // draw Uni Normal
            //drawArrow(g2d, curr.x, curr.y, curr.x + nx*scale, curr.y + ny*scale, Color.BLUE);
        }
    }


    private double computeDiscreteCurvature(Point2D.Double pm1, Point2D.Double p, Point2D.Double pp1) {
        double vx1 = p.x   - pm1.x;
        double vy1 = p.y   - pm1.y;
        double vx2 = pp1.x - p.x;
        double vy2 = pp1.y - p.y;
        double angle1 = Math.atan2(vy1, vx1);
        double angle2 = Math.atan2(vy2, vx2);
        double dtheta = angle2 - angle1;

        while (dtheta >  Math.PI) dtheta -= 2.0*Math.PI;
        while (dtheta <= -Math.PI) dtheta += 2.0*Math.PI;
        return dtheta;
    }


    private void drawArrow(Graphics2D g2d, double x1, double y1, double x2, double y2, Color col) {
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(1.5f/scalingfactor));
        g2d.setColor(col);

        g2d.draw(new Line2D.Double(x1, y1, x2, y2));

        double dx = x2 - x1;
        double dy = y2 - y1;
        double theta = Math.atan2(dy, dx);
        double arrowLen = 0.01; //superior
        //double arrowLen = 5.0;//key
        //double arrowLen = 0.08;//disk
        double angle = Math.toRadians(20); 

        double xA = x2 - arrowLen * Math.cos(theta + angle);
        double yA = y2 - arrowLen * Math.sin(theta + angle);
        double xB = x2 - arrowLen * Math.cos(theta - angle);
        double yB = y2 - arrowLen * Math.sin(theta - angle);

        g2d.draw(new Line2D.Double(x2, y2, xA, yA));
        g2d.draw(new Line2D.Double(x2, y2, xB, yB));

        g2d.setStroke(oldStroke);
    }

    private void evolveByDiscreteObjectiveFlow(double dt) {

        for (List<Point2D.Double> comp : components) {
            if (comp.size() < 3) continue;

            List<Point2D.Double> gradients = new ArrayList<>(Collections.nCopies(comp.size(), null));

            for (int i = 0; i < comp.size(); i++) {
                Point2D.Double p_im1 = comp.get((i - 1 + comp.size()) % comp.size()); 
                Point2D.Double p_i   = comp.get(i);                                   
                Point2D.Double p_ip1 = comp.get((i + 1) % comp.size());               

                double dist_im1_i = dist(p_im1.x, p_im1.y, p_i.x, p_i.y);

                double dist_i_ip1 = dist(p_i.x, p_i.y, p_ip1.x, p_ip1.y);

                double gx = 0.0, gy = 0.0;
                if (dist_im1_i > 1e-12) {
                    gx += (p_i.x - p_im1.x) / dist_im1_i;
                    gy += (p_i.y - p_im1.y) / dist_im1_i;
                }
                if (dist_i_ip1 > 1e-12) {
                    gx += (p_i.x - p_ip1.x) / dist_i_ip1;
                    gy += (p_i.y - p_ip1.y) / dist_i_ip1;
                }

                gradients.set(i, new Point2D.Double(gx, gy));
            }

            for (int i = 0; i < comp.size(); i++) {
                Point2D.Double p_i = comp.get(i);
                Point2D.Double grad = gradients.get(i);
                double newX = p_i.x - dt * grad.x;  
                double newY = p_i.y - dt * grad.y;
                comp.set(i, new Point2D.Double(newX, newY));
            }
        }
    }

    private double dist(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx*dx + dy*dy);
    }
}
