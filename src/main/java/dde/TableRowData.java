package dde;

import javafx.beans.property.*;

public class TableRowData {
    private final DoubleProperty t = new SimpleDoubleProperty();
    private final DoubleProperty y = new SimpleDoubleProperty();
    private final DoubleProperty yTau1 = new SimpleDoubleProperty();
    private final DoubleProperty yTau2 = new SimpleDoubleProperty();

    public TableRowData(double t, double y, double yTau1, double yTau2) {
        this.t.set(t);
        this.y.set(y);
        this.yTau1.set(yTau1);
        this.yTau2.set(yTau2);
    }

    public DoubleProperty tProperty() { return t; }
    public DoubleProperty yProperty() { return y; }
    public DoubleProperty yTau1Property() { return yTau1; }
    public DoubleProperty yTau2Property() { return yTau2; }
}
