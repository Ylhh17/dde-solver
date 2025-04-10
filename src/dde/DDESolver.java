package dde;

import java.util.ArrayList;

public class DDESolver {

    // Начальная функция φ(t) на [-τ, 0]
    public double phi(double t) {
        return 1.0;  // например, постоянная 1
    }

    // Правая часть уравнения: y'(t) = -a * y(t - τ)
    public double f(double t, double yDelay, double a) {
        return -a * yDelay;
    }

    public ArrayList<Double> getTList() {
        return tList;
    }

    public ArrayList<Double> getYList() {
        return yList;
    }

    private ArrayList<Double> tList = new ArrayList<>();
    private ArrayList<Double> yList = new ArrayList<>();

    public void solve(double a, double tau, double h, double tMax) {

        int steps = (int) (tMax / h);
        for (int i = 0; i <= steps; i++) {
            double t = i * h;

            double y;
            if (t <= 0) {
                y = phi(t);
            } else {
                double tDelay = t - tau;

                // интерполяция для y(t - τ)
                double yDelay;
                if (tDelay <= 0) {
                    yDelay = phi(tDelay);
                } else {
                    int index = (int) (tDelay / h);
                    double t1 = tList.get(index);
                    double t2 = tList.get(index + 1);
                    double y1 = yList.get(index);
                    double y2 = yList.get(index + 1);

                    // линейная интерполяция
                    yDelay = y1 + (y2 - y1) * (tDelay - t1) / (t2 - t1);
                }

                double yPrev = yList.get(yList.size() - 1);
                y = yPrev + h * f(t, yDelay, a);
            }

            tList.add(t);
            yList.add(y);

            System.out.printf("t = %.2f, y = %.5f\n", t, y);
        }
    }
}
